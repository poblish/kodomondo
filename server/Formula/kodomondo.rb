require 'formula'

class Kodomondo < Formula
  homepage 'https://github.com/poblish/kodomondo'
  url "https://github.com/poblish/kodomondo/raw/mvn-repo/com/andrewregan/kodomondo/0.0.1/kodomondo-server-0.0.1.jar"
  sha1 "f9867365ae7a3e201c6b6afc4c1e4d478dd6cc0c"

  resource 'startupScript' do
    url 'https://raw.githubusercontent.com/poblish/kodomondo/master/server/bin/kodomondo'
    sha1 '776035da4f7a78e6e663fccd97b99db50c6bc01e'
  end

  resource 'startupScriptSh' do
    url 'https://raw.githubusercontent.com/poblish/kodomondo/master/server/bin/kodomondo.sh'
    sha1 '776035da4f7a78e6e663fccd97b99db50c6bc01e'
  end

  resource 'configScript' do
    url 'https://raw.githubusercontent.com/poblish/kodomondo/master/server/config/ds.yaml'
    sha1 '6e77a1deaaf6cab5e144164319de64b928b6164c'
  end

  def install
    resource("startupScriptSh").stage { bin.install "kodomondo.sh" }
    resource("startupScript").stage { bin.install "kodomondo" }  # FIXME Should be link to kodomondo.sh!
    resource("configScript").stage { (prefix/"config").install "ds.yaml" }
    libexec.install Dir['*']
    # bin.install_symlink "#{bin}/kodomondo.sh" => "kodomondo"
  end
end