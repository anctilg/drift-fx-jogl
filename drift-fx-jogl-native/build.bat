rem (if exist win-x64 rmdir /S /Q win-x64) && cmake -A x64 -S . -B ./win-x64 && cmake --build ./win-x64 --config %1
cmake -A x64 -S . -B ./win-x64 && cmake --build ./win-x64 --config %1