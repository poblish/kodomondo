require "socket"
require "json"

webserver = TCPServer.new('localhost', 2000)
base_dir = Dir.new(".")
while (session = webserver.accept)
  request = session.gets
  resource = File.expand_path('~/.m2/repository/' + request.gsub(/GET\ \//, '').gsub(/\ HTTP.*/, '').chomp)

  if !File.exists?(resource)
    session.print "HTTP/1.1 404/Object Not Found\r\nkodomondo Server\r\n\r\n"
    session.close
    next
  end

  session.print "HTTP/1.1 200/OK\r\nContent-type:application/json\r\n\r\n"

  if File.directory?(resource)
    if resource == ""
      base_dir = Dir.new(".")
    else
      base_dir = Dir.new(resource)
    end

    base_dir.entries.each do |f|
      dir_sign = ""
      base_path = resource + "/"
      base_path = "" if resource == ""
      resource_path = base_path + f
#    session.print('{' + resource_path + '}');

      if File.directory?(resource_path)
        dir_sign = "/"
      end
      if f == ".."
        upper_dir = base_path.split("/")[0..-2].join("/")
        # session.print("<a href=\"xxx\"></a>")
      else
        # session.print("<a href=\"xxx\"></a>")
      end
    end

    @b = base_dir.entries.map { |e| {'file' => e} }
    session.print( @b );

  else
    session.print( JSON.generate({'file' => resource}) );
  end
  session.close
end