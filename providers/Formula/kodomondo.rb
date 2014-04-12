require 'formula'

class Kodomondo < Formula
  homepage 'https://github.com/poblish/kodomondo'
  url "https://github.com/poblish/kodomondo/raw/mvn-repo/com/andrewregan/kodomondo/0.0.1/local-maven-server-0.0.1.jar"
  sha1 "ffd0307959ebaac1b45abc63b17fb924733c6b50"

  resource 'startupScript' do
    url 'https://raw.githubusercontent.com/poblish/kodomondo/master/providers/bin/kodomondo.sh'
    sha1 '35bab5dbc5fb3457b07e323e1281d15126d603c3'
  end

  resource 'configScript' do
    url 'https://raw.githubusercontent.com/poblish/kodomondo/master/providers/config/ds.yaml'
    sha1 '6e77a1deaaf6cab5e144164319de64b928b6164c'
  end

  def install
    resource("startupScript").stage { bin.install "kodomondo.sh" }  # FIXME Need to install 'kodomondo'
    resource("configScript").stage { (prefix/"config").install "ds.yaml" }
    libexec.install Dir['*']
    # bin.install_symlink "#{bin}/kodomondo.sh" => "kodomondo"
  end
end