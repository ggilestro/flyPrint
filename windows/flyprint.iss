; Inno Setup script for FlyPrint Windows installer
; Build with: iscc flyprint.iss
;
; Expects PyInstaller output in ..\dist\FlyPrint\
; and SumatraPDF portable in .\SumatraPDF\SumatraPDF.exe

#define MyAppName "FlyPrint"
#define MyAppVersion "0.1.1"
#define MyAppPublisher "Giorgio Gilestro"
#define MyAppURL "https://www.flyroom.net"
#define MyAppExeName "flyprint-gui.exe"
#define MyAppCLIName "flyprint.exe"

[Setup]
AppId={{B3D7F8A2-4E5C-4A1B-9D6E-8F2A3B4C5D6E}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
AllowNoIcons=yes
OutputBaseFilename=FlyPrint-Setup-{#MyAppVersion}
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
SetupIconFile=flyprint.ico
UninstallDisplayIcon={app}\flyprint-gui.exe
MinVersion=10.0
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
Name: "startupicon"; Description: "Start FlyPrint on login"; GroupDescription: "Other:"

[Files]
; PyInstaller output (one-folder mode)
Source: "..\dist\FlyPrint\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
; SumatraPDF portable (bundled for PDF printing)
; Place SumatraPDF.exe in windows\SumatraPDF\ before building the installer
Source: "SumatraPDF\*"; DestDir: "{app}\SumatraPDF"; Flags: ignoreversion recursesubdirs createallsubdirs skipifsourcedoesntexist

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\{#MyAppName} CLI"; Filename: "{app}\{#MyAppCLIName}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Registry]
; Start on login (only when task selected)
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "FlyPrint"; ValueData: """{app}\{#MyAppExeName}"""; Flags: uninsdeletevalue; Tasks: startupicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

