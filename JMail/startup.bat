cd "WEB-INF"
mkdir "classes"
cd ".."

"%JAVA_HOME%\bin\javac" -d "WEB-INF/classes/" @sources.txt
echo "All Files compiled successfully"
pause