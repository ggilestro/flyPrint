class Flyprint < Formula
  include Language::Python::Virtualenv

  desc "Local print agent for FlyRoom label printing via CUPS"
  homepage "https://github.com/ggilestro/flyPrint"
  url "https://github.com/ggilestro/flyPrint/archive/refs/tags/v0.1.0.tar.gz"
  sha256 "9c1a7fa79244623264842ca443e73b7939f335c91f6a64dc1ffed5604bbc4a1a"
  license "MIT"
  head "https://github.com/ggilestro/flyPrint.git", branch: "master"

  depends_on "python@3.13"
  depends_on "jpeg-turbo"  # For Pillow
  depends_on "libtiff"     # For Pillow
  depends_on "little-cms2" # For Pillow
  depends_on :macos # CUPS is built-in on macOS

  # --- Core dependencies ---

  resource "click" do
    url "https://files.pythonhosted.org/packages/3d/fa/656b739db8587d7b5dfa22e22ed02566950fbfbcdc20311993483657a5c0/click-8.3.1.tar.gz"
    sha256 "12ff4785d337a1bb490bb7e9c2b1ee5da3112e94a8622f26a6c77f5d2fc6842a"
  end

  resource "requests" do
    url "https://files.pythonhosted.org/packages/c9/74/b3ff8e6c8446842c3f5c837e9c3dfcfe2018ea6ecef224c710c85ef728f4/requests-2.32.5.tar.gz"
    sha256 "dbba0bac56e100853db0ea71b82b4dfd5fe2bf6d3754a8893c3af500cec7d7cf"
  end

  resource "certifi" do
    url "https://files.pythonhosted.org/packages/e0/2d/a891ca51311197f6ad14a7ef42e2399f36cf2f9bd44752b3dc4eab60fdc5/certifi-2026.1.4.tar.gz"
    sha256 "ac726dd470482006e014ad384921ed6438c457018f4b3d204aea4281258b2120"
  end

  resource "charset-normalizer" do
    url "https://files.pythonhosted.org/packages/13/69/33ddede1939fdd074bce5434295f38fae7136463422fe4fd3e0e89b98062/charset_normalizer-3.4.4.tar.gz"
    sha256 "94537985111c35f28720e43603b8e7b43a6ecfb2ce1d3058bbe955b73404e21a"
  end

  resource "idna" do
    url "https://files.pythonhosted.org/packages/6f/6d/0703ccc57f3a7233505399edb88de3cbd678da106337b9fcde432b65ed60/idna-3.11.tar.gz"
    sha256 "795dafcc9c04ed0c1fb032c2aa73654d8e8c5023a7df64a53f39190ada629902"
  end

  resource "urllib3" do
    url "https://files.pythonhosted.org/packages/c7/24/5f1b3bdffd70275f6661c76461e25f024d5a38a46f04aaca912426a2b1d3/urllib3-2.6.3.tar.gz"
    sha256 "1b62b6884944a57dbe321509ab94fd4d3b307075e0c2eae991ac71ee15ad38ed"
  end

  # --- CUPS printing ---

  resource "pycups" do
    url "https://files.pythonhosted.org/packages/96/c4/b077f0422cd031e4f3a47c75ce0bcf77f2f2e5bf3648f6945a4d09fd44a5/pycups-2.0.4.tar.gz"
    sha256 "843e385c1dbf694996ca84ef02a7f30c28376035588f5fbeacd6bae005cf7c8d"
  end

  # --- GUI (system tray) ---

  resource "pystray" do
    url "https://files.pythonhosted.org/packages/5c/64/927a4b9024196a4799eba0180e0ca31568426f258a4a5c90f87a97f51d28/pystray-0.19.5-py2.py3-none-any.whl"
    sha256 "a0c2229d02cf87207297c22d86ffc57c86c227517b038c0d3c59df79295ac617"
  end

  resource "Pillow" do
    url "https://files.pythonhosted.org/packages/1f/42/5c74462b4fd957fcd7b13b04fb3205ff8349236ea74c7c375766d6c82288/pillow-12.1.1.tar.gz"
    sha256 "9ad8fa5937ab05218e2b6a4cff30295ad35afd2f83ac592e68c0d871bb0fdbc4"
  end

  resource "six" do
    url "https://files.pythonhosted.org/packages/94/e7/b2c673351809dca68a0e064b6af791aa332cf192da575fd474ed7d6f16a2/six-1.17.0.tar.gz"
    sha256 "ff70335d468e7eb6ec65b95b99d3a2836546063f63acc5171de367e834932a81"
  end

  # --- pystray macOS backend (pyobjc) ---

  resource "pyobjc-core" do
    url "https://files.pythonhosted.org/packages/b8/b6/d5612eb40be4fd5ef88c259339e6313f46ba67577a95d86c3470b951fce0/pyobjc_core-12.1.tar.gz"
    sha256 "2bb3903f5387f72422145e1466b3ac3f7f0ef2e9960afa9bcd8961c5cbf8bd21"
  end

  resource "pyobjc-framework-Cocoa" do
    url "https://files.pythonhosted.org/packages/02/a3/16ca9a15e77c061a9250afbae2eae26f2e1579eb8ca9462ae2d2c71e1169/pyobjc_framework_cocoa-12.1.tar.gz"
    sha256 "5556c87db95711b985d5efdaaf01c917ddd41d148b1e52a0c66b1a2e2c5c1640"
  end

  resource "pyobjc-framework-Quartz" do
    url "https://files.pythonhosted.org/packages/94/18/cc59f3d4355c9456fc945eae7fe8797003c4da99212dd531ad1b0de8a0c6/pyobjc_framework_quartz-12.1.tar.gz"
    sha256 "27f782f3513ac88ec9b6c82d9767eef95a5cf4175ce88a1e5a65875fee799608"
  end

  def install
    virtualenv_install_with_resources
  end

  service do
    run [opt_bin/"flyprint", "start"]
    keep_alive true
    log_path var/"log/flyprint.log"
    error_log_path var/"log/flyprint.log"
    working_dir HOMEBREW_PREFIX
  end

  def caveats
    <<~EOS
      To get started:

        1. Pair with your FlyRoom server:
             flyprint pair https://app.flyroom.net

        2. Print a test label:
             flyprint test

        3. Run as a background service:
             brew services start flyprint

      The GUI (system tray) can be launched with:
             flyprint-gui

      Logs are written to:
             #{var}/log/flyprint.log
    EOS
  end

  test do
    assert_match version.to_s, shell_output("#{bin}/flyprint --version")
  end
end
