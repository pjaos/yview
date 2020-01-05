EESchema Schematic File Version 4
EELAYER 30 0
EELAYER END
$Descr A4 11693 8268
encoding utf-8
Sheet 1 1
Title "YSIGGEN"
Date "2019-12-30"
Rev "1.0"
Comp ""
Comment1 ""
Comment2 ""
Comment3 ""
Comment4 ""
$EndDescr
Text Notes 5850 3350 0    50   ~ 0
GND
Text Notes 5850 3450 0    50   ~ 0
VCC
Text Notes 5850 3550 0    50   ~ 0
NC
Text Notes 5850 3650 0    50   ~ 0
NC
Text Notes 5850 3750 0    50   ~ 0
MUXOUT
Text Notes 5850 3850 0    50   ~ 0
CLK
Text Notes 5850 3950 0    50   ~ 0
DATA
Text Notes 5850 4050 0    50   ~ 0
LE
Text Notes 5850 4150 0    50   ~ 0
CE
Text Notes 5850 4250 0    50   ~ 0
GND
Text Notes 5550 4600 0    50   ~ 0
ADF4350 \nMODULE \nCONNECTOR
$Comp
L pja_1:ESP32_NODEMCU U1
U 1 1 5E0B9BD5
P 3800 3900
F 0 "U1" H 3775 5115 50  0000 C CNN
F 1 "ESP32_NODEMCU" H 3775 5024 50  0000 C CNN
F 2 "" H 3800 3850 50  0001 C CNN
F 3 "" H 3800 3850 50  0001 C CNN
	1    3800 3900
	1    0    0    -1  
$EndComp
$Comp
L Connector_Generic:Conn_01x10 J1
U 1 1 5E0A6742
P 5750 3750
F 0 "J1" H 5700 4250 50  0000 L CNN
F 1 "Conn_01x10" H 5830 3651 50  0001 L CNN
F 2 "" H 5750 3750 50  0001 C CNN
F 3 "~" H 5750 3750 50  0001 C CNN
	1    5750 3750
	1    0    0    -1  
$EndComp
Wire Wire Line
	3000 4250 2450 4250
Wire Wire Line
	4850 3550 4550 3550
Wire Wire Line
	5550 3350 5150 3350
Wire Wire Line
	5150 3350 5150 3550
Wire Wire Line
	5150 3550 4850 3550
Connection ~ 4850 3550
Wire Wire Line
	5550 3450 5250 3450
Wire Wire Line
	5250 3450 5250 4950
Wire Wire Line
	2850 4950 2850 4750
Wire Wire Line
	2850 4750 3000 4750
Wire Wire Line
	5550 4250 4850 4250
Connection ~ 4850 4250
Wire Wire Line
	4850 4250 4850 3550
Wire Wire Line
	5550 4150 4550 4150
Wire Wire Line
	5550 4050 4750 4050
Wire Wire Line
	4750 4050 4750 3850
Wire Wire Line
	4750 3850 4550 3850
Wire Wire Line
	5550 3950 4950 3950
Wire Wire Line
	4950 3950 4950 3050
Wire Wire Line
	4950 3050 4550 3050
Wire Wire Line
	5550 3850 5100 3850
Wire Wire Line
	5100 3850 5100 3750
Wire Wire Line
	5100 3750 4550 3750
Wire Wire Line
	5550 3750 5200 3750
Wire Wire Line
	5200 3750 5200 3650
Wire Wire Line
	5200 3650 4550 3650
Wire Wire Line
	5250 4950 2850 4950
Wire Wire Line
	4850 5350 4850 4250
Wire Wire Line
	2450 4250 2450 5350
Wire Wire Line
	2450 5350 4850 5350
$EndSCHEMATC
