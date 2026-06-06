@echo off
rem run-app.bat - thin wrapper for run-app.ps1
rem Lets you double-click or call from any cmd/PowerShell session.
rem The Bypass policy avoids the "running scripts is disabled" error
rem on stock Windows machines.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-app.ps1"
