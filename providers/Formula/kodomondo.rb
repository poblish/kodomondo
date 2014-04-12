require 'formula'

class Kodomondo < Formula
  homepage 'https://github.com/poblish/kodomondo'
  url "https://github.com/poblish/kodomondo/raw/mvn-repo/com/andrewregan/kodomondo/0.0.1/local-maven-server-0.0.1.jar"
  sha1 "ffd0307959ebaac1b45abc63b17fb924733c6b50"

  resource 'startupScript' do
    url 'https://raw.githubusercontent.com/poblish/kodomondo/master/providers/bin/kodomondo.sh'
    sha1 'd8ebcff7dda5ff0b418a4685dbe99e0148ffc957'
  end

  def install
    resource("startupScript").stage { bin.install "kodomondo.sh" }  # FIXME Need to install 'kodomondo'
    libexec.install Dir['*']
    # bin.install_symlink "#{bin}/kodomondo.sh" => "kodomondo"
  end
end