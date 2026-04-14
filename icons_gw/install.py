#!/usr/bin/env python3
"""
Cross-platform installer/uninstaller for python wheels.
Features:
- Auto-detect version from wheel filename (optional --version)
- User vs system mode
- Multiple version support
- Linux/macOS wrapper scripts with CLI args forwarding
- Windows .bat launchers
- Automatic gui launcher icon creation on Linux, Windows and macos
- install, uninstall, status and switch commands
"""

import argparse
import json
import platform
import re
import shutil
import subprocess
import sys
from pathlib import Path


class Installer:
    APP_NAME = None
    CMD_DICT = None

    ENV_KEY = r"SYSTEM\CurrentControlSet\Control\Session Manager\Environment"

    DISPLAY_ATTR_BRIGHT = 1
    DISPLAY_ATTR_FG_GREEN = 32
    DISPLAY_ATTR_FG_RED = 31
    DISPLAY_RESET_ESCAPE_SEQ = "\x1b[0m"

    INSTALL_ARG = "install"
    UNINSTALL_ARG = "uninstall"
    STATUS_ARG = "status"
    SWITCH_ARG = "switch"
    ALL_COMMANDS = (INSTALL_ARG, UNINSTALL_ARG, STATUS_ARG, SWITCH_ARG)

    HELP_ARG_1 = '-h'
    HELP_ARG_2 = '--help'
    HELP_ARGS = (HELP_ARG_1, HELP_ARG_2)

    @staticmethod
    def GetInfoEscapeSeq():
        """@return the info level ANSI escape sequence."""
        return "\x1b[{:01d};{:02d}m".format(Installer.DISPLAY_ATTR_FG_GREEN, Installer.DISPLAY_ATTR_BRIGHT)

    @staticmethod
    def GetErrorEscapeSeq():
        """@return the warning level ANSI escape sequence."""
        return "\x1b[{:01d};{:02d}m".format(Installer.DISPLAY_ATTR_FG_RED, Installer.DISPLAY_ATTR_BRIGHT)

    def __init__(self, handle_cmd_line=True, color=True):
        """@brief Constructor
           @param handle_cmd_line If True then the command lines arguments are processed in the constructor.
                                  If False then the following methods should be called after the caller
                                  gets a reference to an instance of this class.

                                  parse_args()
                                  process_cmdline()
                                  """
        self._colour = color
        if self.APP_NAME is None or self.CMD_DICT is None:
            raise Exception("BUG: Installer.APP_NAME and Installer.CMD_DICT must be defined in subclass of the Installer class.")

        if handle_cmd_line:
            self.parse_args()
            self.process_cmdline()

    def info(self, text):
        """@brief Present an info level message to the user.
           @param text The line of text to be presented to the user."""
        if self._colour:
            print('{}INFO{}:  {}'.format(Installer.GetInfoEscapeSeq(), Installer.DISPLAY_RESET_ESCAPE_SEQ, text))
        else:
            print('INFO:  {}'.format(text))

    def error(self, text):
        """@brief Present an error level message to the user.
           @param text The line of text to be presented to the user."""
        if self._colour:
            print('{}ERROR{}: {}'.format(Installer.GetErrorEscapeSeq(), Installer.DISPLAY_RESET_ESCAPE_SEQ, text), file=sys.stderr)
        else:
            print('ERROR: {}'.format(text), file=sys.stderr)

    def parse_args(self):
        # Check to see if the user entered a command
        user_help_request = False
        if set(Installer.HELP_ARGS) & set(sys.argv):
            user_help_request = True

        if not user_help_request:
            self._cmd_found = False
            for cmd in Installer.ALL_COMMANDS:
                if cmd in sys.argv:
                    self._cmd_found = True
                    break

            # If no command was entered force an install command.
            if not self._cmd_found:
                sys.argv.insert(1, Installer.INSTALL_ARG)

        parser = argparse.ArgumentParser(description=f"{self.APP_NAME}: install is the default command.")
        sub = parser.add_subparsers(dest="command", required=True)

        # Install
        p = sub.add_parser(Installer.INSTALL_ARG)
        p.add_argument("wheel", help="Path to the Python wheel (.whl)")
        p.add_argument("--version", help="Version being installed (auto-detected if omitted)", default=None)
        p.add_argument("--base", help="Installation base path", default=str(Path.home() / f".{self.APP_NAME}"))
        p.add_argument("--mode", choices=["user", "system"], default="user")

        # Uninstall
        p = sub.add_parser(Installer.UNINSTALL_ARG)
        p.add_argument("--all", action="store_true", help="Remove all versions")
        p.add_argument("--version", help="Specific version to remove")
        p.add_argument("--base", help="Installation base path", default=str(Path.home() / f".{self.APP_NAME}"))
        p.add_argument("--mode", choices=["user", "system"], default="user")

        # Status
        p = sub.add_parser(Installer.STATUS_ARG)
        p.add_argument("--base", help="Installation base path", default=str(Path.home() / f".{self.APP_NAME}"))
        p.add_argument("--json", action="store_true", help="JSON output")
        p.add_argument("--mode", choices=["user", "system"], default="user")

        # Switch
        p = sub.add_parser(Installer.SWITCH_ARG)
        p.add_argument("version", nargs="?", help="Version to activate")
        p.add_argument("--latest", action="store_true", help="Switch to highest installed version")
        p.add_argument("--base", default=str(Path.home() / f".{self.APP_NAME}"))
        p.add_argument("--mode", choices=["user", "system"], default="user")

        self.args = parser.parse_args()

    def process_cmdline(self):
        if self.args.command == "install":
            self.install()

        elif self.args.command == "uninstall":
            self.uninstall()

        elif self.args.command == "status":
            self.status()

        elif self.args.command == "switch":
            self.switch_version()

        else:
            self.die("Unknown command")

    def die(self, msg):
        self.error(f"{msg}")
        sys.exit(1)

    def get_bin_dir(self, mode):
        system = platform.system()
        if system == "Windows":
            return (
                Path.home() / "AppData" / "Local" / "Programs" / self.APP_NAME / "bin"
                if mode == "user"
                else Path("C:/Program Files") / self.APP_NAME / "bin"
            )
        else:
            return Path.home() / ".local" / "bin" if mode == "user" else Path("/usr/local/bin")

    def get_desktop_dir(self):
        return Path.home() / ".local" / "share" / "applications"

    def get_macos_app_dir(self):
        return Path.home() / "Applications"   # this is where your installer puts .app

    def all_versions(self, base):
        return sorted(
            d.name for d in base.iterdir()
            if d.is_dir() and d.name != "current"
        )

    def detect_version_from_wheel(self, wheel_path: Path):
        # Example: mpy_tool-0.45-py3-none-any.whl → 0.45
        m = re.search(rf"{self.APP_NAME}-(\d+(?:\.\d+)*)", wheel_path.name)
        if not m:
            self.die(f"Could not auto-detect version from wheel filename '{wheel_path.name}'")
        return m.group(1)

    def select_version(self, base: Path, requested: str | None, latest: bool):
        versions = self.all_versions(base)
        if not versions:
            self.die("No versions installed")

        if latest:
            return versions[-1]

        if not requested:
            self.die("Specify a version or --latest")

        if requested not in versions:
            self.die(f"Version {requested} is not installed")

        return requested

    def remove_active_launchers(self, base: Path, mode: str):
        """
        Remove all launchers that point into ~/.mpy_tool.
        Works even if install.json is missing.
        """
        bin_dir = self.get_bin_dir(mode)
        system = platform.system()

        if not bin_dir.exists():
            return

        for p in bin_dir.iterdir():
            if system == "Windows" and p.suffix == ".bat":
                txt = p.read_text(errors="ignore")
                if str(base) in txt:
                    p.unlink()
            else:
                if p.is_symlink():
                    try:
                        resolved = p.resolve()
                        if resolved.is_relative_to(base):
                            p.unlink()
                    except Exception:
                        pass

    def remove_active_gui_launchers(self, base: Path):
        system = platform.system()

        if system == "Linux":
            d = self.get_desktop_dir()
            if d.exists():
                for f in d.glob("*.desktop"):
                    txt = f.read_text(errors="ignore")
                    if str(base) in txt:
                        f.unlink()

        if system == "Darwin":
            d = self.get_macos_app_dir()
            if d.exists():
                for app in d.glob("*.app"):
                    shutil.rmtree(app, ignore_errors=True)

    def switch_version(self):
        base = Path(self.args.base).resolve()
        version = self.select_version(base, self.args.version, self.args.latest)

        self.info(f"Switching {self.APP_NAME} to version {version}")

        # Remove current global launchers
        self.remove_active_launchers(base, self.args.mode)
        self.remove_active_gui_launchers(base)

        venv = base / version / "venv"
        if not venv.exists():
            self.die(f"Broken install: {venv} missing")

        # Recreate launchers for this version
        self.create_launchers(base, version, venv)

        # Update ptr to current version
        self.set_current_version(base, version)

        self.info(f"{self.APP_NAME} now using version {version}")

    def create_venv(self, venv_path: Path, python=sys.executable):
        if not venv_path.exists():
            subprocess.check_call([python, "-m", "venv", str(venv_path)])

    def install_wheel(self, venv_path: Path, wheel: Path):
        python_exe = venv_path / ("Scripts/python.exe" if platform.system() == "Windows" else "bin/python")
        subprocess.check_call([str(python_exe), "-m", "pip", "install", "--upgrade", str(wheel)])

    def remove_launchers_for_version(self, base, version, mode):
        bin_dir = self.get_bin_dir(mode)
        desktop_dir = self.get_desktop_dir()
        mac_app_dir = self.get_macos_app_dir()

        meta_file = base / version / "install.json"
        if not meta_file.exists():
            return

        meta = json.loads(meta_file.read_text())
        cmds = meta["commands"]

        for cmd in cmds:
            # Linux/macOS shell wrappers
            p = bin_dir / cmd
            if p.exists() or p.is_symlink():
                try:
                    target = p.resolve()
                    if str(base / version) in str(target):
                        p.unlink()
                except Exception:
                    pass

            # Linux .desktop files
            desktop = desktop_dir / f"{cmd}.desktop"
            if desktop.exists():
                desktop.unlink()

            # macOS .app bundles
            app = mac_app_dir / f"{cmd}.app"
            if app.exists():
                shutil.rmtree(app, ignore_errors=True)

    def remove_windows_launchers(self, mode):
        bin_dir = self.get_bin_dir(mode)
        if not bin_dir.exists():
            return
        for cmd in self.CMD_DICT:
            bat = bin_dir / f"{cmd}.bat"
            if bat.exists():
                bat.unlink()

    def remove_from_user_path(self, dir_to_remove):
        dir_to_remove = str(dir_to_remove).lower().rstrip("\\")
        current = self.get_user_path()
        parts = [p for p in current.split(";") if p]

        new_parts = []
        for p in parts:
            if p.lower().rstrip("\\") != dir_to_remove:
                new_parts.append(p)

        new = ";".join(new_parts)
        if new != current:
            self.set_user_path(new)
            return True
        return False

    def load_install_record(self, version_path: Path):
        f = version_path / "install.json"
        if not f.exists():
            self.die(f"Missing install.json in {version_path}")
        return json.loads(f.read_text())

    def get_installed_commands(self, version_path: Path):
        """
        Return list of commands belonging to this version.
        Works even if install.json is missing.
        """
        meta = version_path / "install.json"
        if meta.exists():
            try:
                data = json.loads(meta.read_text())
                return data.get("commands", [])
            except Exception:
                pass

        # Fallback: inspect venv/bin
        venv = version_path / "venv"
        if platform.system() == "Windows":
            bin_dir = venv / "Scripts"
            exts = (".exe", ".bat", ".cmd")
        else:
            bin_dir = venv / "bin"
            exts = ("",)

        cmds = []
        if bin_dir.exists():
            for p in bin_dir.iterdir():
                for ext in exts:
                    if p.name == p.stem + ext:
                        if p.stem in self.CMD_DICT:
                            cmds.append(p.stem)

        # Final fallback (very old installs)
        return list(self.CMD_DICT.keys())

    def _is_launcher_required(self, cmd):
        """@brief Determine if a launcher icon is required.
           @param cmd The cmd to check.
           @return True if required."""
        launcher_required = False
        if cmd in self.CMD_DICT:
            attr_list = self.CMD_DICT[cmd]
            launcher_required = attr_list[1]
        return launcher_required

    def remove_version(self, version: str, base: Path, mode: str):
        version_path = base / version
        if not version_path.exists():
            self.info(f"Version {version} not found")
            return

        system = platform.system()
        bin_dir = self.get_bin_dir(mode)
        mac_app_dir = self.get_macos_app_dir()

        commands = self.get_installed_commands(version_path)

        for cmd in commands:
            # ----- CLI launchers -----
            launcher = bin_dir / cmd
            if system == "Windows" and not launcher.name.endswith(".bat"):
                launcher = launcher.with_name(launcher.name + ".bat")
            if launcher.exists() or launcher.is_symlink():

                # If gui is in the cmd name then we would have tried to create a icon launcher when it was installed.
                if self._is_launcher_required(cmd):
                    # Try running it with the --remove_launcher argument (see p3lib launcher.py)
                    # to remove and launcher created previously.
                    try:
                        subprocess.check_call([launcher, "--remove_launcher"])
                    except Exception:
                        # Fail silently as cmd may not support the create gui launcher functionality
                        pass

                try:
                    target = launcher.resolve()
                    # If this is a link to a file in the venv
                    if str(version_path) in str(target):
                        launcher.unlink()
                        self.info(f"Removed {launcher}")

                    # If this is a startup file in the ~/.local folder
                    elif target.is_file():
                        launcher.unlink()
                        self.info(f"Removed {launcher}")

                except Exception:
                    launcher.unlink(missing_ok=True)

            # Windows .bat
            if system == "Windows":
                bat = bin_dir / f"{cmd}.bat"
                if bat.exists():
                    txt = bat.read_text(errors="ignore")
                    if str(version_path) in txt:
                        bat.unlink()
                        self.info(f"Removed {bat}")

            # macOS .app
            if system == "Darwin":
                app = mac_app_dir / f"{cmd}.app"
                if app.exists():
                    shutil.rmtree(app, ignore_errors=True)
                    self.info(f"Removed {app}")

        shutil.rmtree(version_path, ignore_errors=True)
        self.info(f"Removed version {version}")

    def uninstall(self):
        base = Path(self.args.base).resolve()

        if not base.exists():
            self.info("Nothing installed")
            return

        if self.args.all:
            for v in self.all_versions(base):
                self.remove_version(v, base, self.args.mode)
            return

        if self.args.version:
            self.remove_version(self.args.version, base, self.args.mode)
            return

        self.die("Specify --all or --version")

    def get_machine_path(self):
        import winreg
        with winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, Installer.ENV_KEY) as k:
            return winreg.QueryValueEx(k, "Path")[0]

    def get_user_path(self):
        import winreg
        try:
            with winreg.OpenKey(winreg.HKEY_CURRENT_USER, "Environment") as k:
                return winreg.QueryValueEx(k, "Path")[0]
        except FileNotFoundError:
            return ""

    def set_user_path(self, value):
        import winreg
        with winreg.OpenKey(winreg.HKEY_CURRENT_USER, "Environment", 0, winreg.KEY_SET_VALUE) as k:
            winreg.SetValueEx(k, "Path", 0, winreg.REG_EXPAND_SZ, value)

    def add_to_user_path(self, dir_to_add):
        # Ensure string
        dir_to_add = str(dir_to_add)

        current = self.get_user_path()

        parts = [p for p in current.split(";") if p]

        norm = [p.lower().rstrip("\\") for p in parts]
        target = dir_to_add.lower().rstrip("\\")

        if target in norm:
            return False   # already present

        new = current + (";" if current and not current.endswith(";") else "") + dir_to_add
        self.set_user_path(new)
        return True

    def ask_reboot(self):
        import ctypes

        MB_ICONQUESTION = 0x20
        MB_YESNO = 0x04
        MB_DEFBUTTON2 = 0x100
        MB_SYSTEMMODAL = 0x1000

        result = ctypes.windll.user32.MessageBoxW(
            None,
            "MPY Tool has been added to your PATH.\n\n"
            "Windows needs to restart Explorer (or reboot) for this to take effect.\n\n"
            "Restart the computer now?",
            "MPY Tool installation",
            MB_ICONQUESTION | MB_YESNO | MB_DEFBUTTON2 | MB_SYSTEMMODAL
        )

        if result == 6:  # YES
            ctypes.windll.shell32.ShellExecuteW(
                None, "runas", "shutdown", "/r /t 5", None, 1
            )

    def create_launchers(self, base: Path, version: str, venv_path: Path):
        """
        Create CLI launchers and Linux .desktop files.

        On Windows: creates .bat files that call the venv-installed console scripts.
        On Linux/macOS: creates wrapper scripts that either call python -m <module>
                        or the venv-installed console scripts, with symlinks in bin_dir.
        """

        system = platform.system()
        bin_dir = self.get_bin_dir(self.args.mode)
        if system == "Windows":
            bin_dir.mkdir(parents=True, exist_ok=True)

            venv_dir = str(venv_path)

            for cmd, attr_list in self.CMD_DICT.items():
                module_target = attr_list[0]
                launcher = bin_dir / f"{cmd}.bat"
                if module_target:
                    launcher.write_text(
                        f"""@echo off
set VENV_DIR={venv_dir}
call "%VENV_DIR%\\Scripts\\activate.bat"
python -m {module_target} %*
""")

                else:
                    launcher.write_text(
                        f"""@echo off
set VENV_DIR={venv_dir}
call "%VENV_DIR%\\Scripts\\activate.bat"
python -m {self.APP_NAME}.{cmd} %*
""")
                self.info(f"Created {launcher}")

            # Ensure the bin folder is on the system PATH
            path_changed = self.add_to_user_path(bin_dir)

            if path_changed:
                self.ask_reboot()

        else:
            # Linux / macOS
            bin_dir.mkdir(parents=True, exist_ok=True)

            wrapper_dir = base / version / "launchers"
            wrapper_dir.mkdir(parents=True, exist_ok=True)

            python_exe = venv_path / "bin" / "python"

            for cmd, attr_list in self.CMD_DICT.items():
                module_target = attr_list[0]
                if module_target:
                    # Command needs python -m module
                    launcher = bin_dir / cmd
                    contents = f"""#!/bin/sh
exec "{python_exe}" -m {module_target} "$@"
"""
                    launcher.write_text(contents)
                    launcher.chmod(0o755)
                else:
                    # Use the venv-installed console script
                    entrypoint = venv_path / "bin" / cmd
                    if not entrypoint.exists():
                        self.die(f"Entrypoint {cmd} not found in venv at {entrypoint}")

                    wrapper_script = wrapper_dir / f"{cmd}.sh"
                    wrapper_script.write_text(f"""#!/bin/sh
exec "{entrypoint}" "$@"
""")
                    wrapper_script.chmod(0o755)

                    launcher = bin_dir / cmd
                    if launcher.exists() or launcher.is_symlink():
                        launcher.unlink()
                    launcher.symlink_to(wrapper_script)

                self.info(f"Created {launcher}")

            # Optional: create .desktop files for GUI commands
            desktop_dir = Path.home() / ".local" / "share" / "applications"
            desktop_dir.mkdir(parents=True, exist_ok=True)

        for cmd, attr_list in self.CMD_DICT.items():
            module_target = attr_list[0]
            # If the command starts a gui
            if self._is_launcher_required(cmd):
                # Try running it with the --add_launcher argument (see p3lib launcher.py)
                # This supports creation of a GUI launcher with an icon on
                # Linux, Windows and macos platforms.
                # On Windows and macos an icon is created on the desktop.
                # On Linux platforms a gnome application launcher is created.
                try:
                    full_cmd = bin_dir / cmd
                    if system == "Windows" and not full_cmd.name.endswith(".bat"):
                        full_cmd = full_cmd.with_name(full_cmd.name + ".bat")
                    if full_cmd.exists():
                        subprocess.check_call([full_cmd, "--add_launcher"])
                except Exception:
                    # Fail silently as cmd may not support the create gui launcher functionality
                    pass

        # Create a file to track ownership of launchers
        meta = {
            "version": version,
            "commands": list(self.CMD_DICT.keys())
        }
        meta_file = base / version / "install.json"
        meta_file.write_text(json.dumps(meta, indent=2))

    def current_link(self, base):
        return base / "current"

    def get_current_version(self, base):
        p = self.current_link(base)
        if not p.exists():
            return None

        try:
            if p.is_symlink():
                return p.resolve().name
            else:
                v = p.read_text().strip()
                return v if v else None
        except Exception:
            return None

    def set_current_version(self, base, version):
        p = self.current_link(base)
        target = base / version

        if platform.system() == "Windows":
            p.write_text(version)
        else:
            if p.exists() or p.is_symlink():
                p.unlink()
            p.symlink_to(target)

    def status(self):
        base = Path(self.args.base).resolve()
        versions = self.all_versions(base)
        current = self.get_current_version(base)

        if self.args.json:
            print(json.dumps({
                "current": current,
                "installed": versions
            }, indent=2))
            return

        if not versions:
            self.info("No versions installed")
            return

        self.info("Installed versions:")
        for v in versions:
            mark = "*" if v == current else " "
            self.info(f" {mark} {v}")

    def ensure_pip(self, venv_path: Path):
        python_exe = venv_path / ("Scripts/python.exe" if platform.system() == "Windows" else "bin/python")
        try:
            subprocess.check_call([str(python_exe), "-m", "pip", "--version"],
                                  stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        except Exception:
            self.info("Installing pip into virtualenv...")
            subprocess.check_call([str(python_exe), "-m", "ensurepip", "--upgrade"])
            subprocess.check_call([str(python_exe), "-m", "pip", "install", "--upgrade", "pip", "setuptools", "wheel"])

    def install(self):
        base = Path(self.args.base).resolve()
        wheel_path = Path(self.args.wheel)
        if not wheel_path.exists():
            self.die(f"Wheel file '{wheel_path}' does not exist")

        # Auto-detect version if not provided
        version = self.args.version or self.detect_version_from_wheel(wheel_path)
        base.mkdir(parents=True, exist_ok=True)
        venv_path = base / version / "venv"

        self.create_venv(venv_path)
        self.ensure_pip(venv_path)
        self.install_wheel(venv_path, wheel_path)
        self.create_launchers(base, version, venv_path)
        self.set_current_version(base, version)
        self.info(f"{self.APP_NAME} version {version} installed successfully")


# The Installer class must be extended to be used.
# The APP_NAME and CMD_DICT attributes must be set.
class IconsGW(Installer):
    # All sections mentioned below must be present in the projects pyproject.toml file.

    # APP_NAME
    # The name of the application as defined in [tool.poetry] sections name parameter
    APP_NAME = "icons_gw"

    # CMD_DICT
    # key = the command as defined in the [tool.poetry.scripts] section
    # value = A list with two elements
    # 0 = This maybe the module_name.main_filename format. This forces the startup script
    #     to use the form 'python -m module_name.main_filename' to start the program.
    #     If left as an empty string then the startup script created in the python wheel
    #     from the pyproject.toml [tool.poetry.scripts] is used.
    # 1 = If True then a launcher icon is created.
    CMD_DICT = {
        "icons_gw": ("", False),
        "icons_json_check": ("", False),
        "mqtt_subscribe": ("", False),
    }


def main():
    # All that is needed is for the extended class to be instantiated.
    IconsGW()


if __name__ == "__main__":
    main()
