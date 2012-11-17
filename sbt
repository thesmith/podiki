java -Dfile.encoding=UTF8 -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=312m -jar `dirname $0`/sbt-launch.jar "$@"

