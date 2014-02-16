require "socket"
require "json"

mvn_root = ('~/.m2' if [nil, ''].include?(ENV['M2_HOME'])) + '/repository/'

webserver = TCPServer.new('localhost', 2000)
base_dir = Dir.new(".")
while (session = webserver.accept)
  request = session.gets

  if request == nil then
    next
  end

  resource = File.expand_path( mvn_root + request.gsub(/GET\ \//, '').gsub(/\ HTTP.*/, '').chomp)

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

    array = []

    base_dir.entries.each do |f|

      if f.start_with?(".")
        next
      end

      dir_sign = ""
      base_path = resource + "/"
      base_path = "" if resource == ""
      resource_path = base_path + f

      if File.directory?(resource_path)
        array.push({ :dir => f })
      else
        array.push({ :file => f })
      end
    end

    session.print( JSON.generate(array) );

  else
    session.print( JSON.generate({'file' => resource}) );
  end
  session.close
end