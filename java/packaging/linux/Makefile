#Get the versions for the programs to be built from the src code
YVIEW_VER := $(shell python get_version.py ../../src/yview/model/Constants.java)

OUTPUT_ROOT_PATH = output

#We no long build the Linux binaries 
#using cxfreeze as they suffer from 
#lib dependancy issues.
all: yview

#Remove all the output files
clean:
	rm -rf $(OUTPUT_ROOT_PATH)
	rm -rf $(OUTPUT_ROOT_PATH)/yview/linux
	rm -rf tmp

#Build the yview package.	
yview:
	mkdir -p $(OUTPUT_ROOT_PATH)/yview/linux/DEBIAN
	mkdir -p $(OUTPUT_ROOT_PATH)/yview/linux/usr/local/bin/yview_files
	mkdir -p $(OUTPUT_ROOT_PATH)/yview/linux/usr/share/applications
	cp ../../yview.jar $(OUTPUT_ROOT_PATH)/yview/linux/usr/local/bin/yview_files
	#unity desktop and icon files
	cp yview_desktop/yview.png $(OUTPUT_ROOT_PATH)/yview/linux/usr/local/bin/yview_files
	cp yview_desktop/yview.desktop $(OUTPUT_ROOT_PATH)/yview/linux/usr/share/applications
	cp vncview_desktop/vncview.png $(OUTPUT_ROOT_PATH)/yview/linux/usr/local/bin/yview_files
	cp vncview_desktop/vncview.desktop $(OUTPUT_ROOT_PATH)/yview/linux/usr/share/applications
	#This debian control file should not contain the Version: line
	cp yview.control $(OUTPUT_ROOT_PATH)/yview/linux/DEBIAN/control
	cp yview.postinst $(OUTPUT_ROOT_PATH)/yview/linux/DEBIAN/postinst
	cp yview.prerm $(OUTPUT_ROOT_PATH)/yview/linux/DEBIAN/prerm
	cp yview.startup $(OUTPUT_ROOT_PATH)/yview/linux/usr/local/bin/yview
	cp vncview.startup $(OUTPUT_ROOT_PATH)/yview/linux/usr/local/bin/vncview
	#Add the version to the debian control file
	echo 'Version: $(YVIEW_VER)' >> $(OUTPUT_ROOT_PATH)/yview/linux/DEBIAN/control
	dpkg-deb -Zgzip -b $(OUTPUT_ROOT_PATH)/yview/linux $(OUTPUT_ROOT_PATH)/yview/yview-$(YVIEW_VER).deb
	rm -rf $(OUTPUT_ROOT_PATH)/yview/linux
	#Create rpm from deb file
	cd $(OUTPUT_ROOT_PATH)/yview/ && sudo alien --to-rpm --scripts yview-$(YVIEW_VER).deb
	#Make tarball containing install files
	dpkg -x $(OUTPUT_ROOT_PATH)/yview/yview-$(YVIEW_VER).deb ./tmp
	cd tmp && tar czvf ../$(OUTPUT_ROOT_PATH)/yview/yview-$(YVIEW_VER).tgz *
	rm -rf tmp
	rm -rf $(OUTPUT_ROOT_PATH)/yview/linux
