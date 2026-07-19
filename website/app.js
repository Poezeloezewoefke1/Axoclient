/**
 * Resolve the latest Windows installer from GitHub Releases (public API,
 * no auth). Graceful fallback: if the API is unavailable or rate-limited,
 * the button links to the releases page instead. Hardened in task P4-03.
 */
(function () {
  var REPO = 'Poezeloezewoefke1/Claud';
  var FALLBACK_URL = 'https://github.com/' + REPO + '/releases/latest';

  var button = document.getElementById('download-button');
  var meta = document.getElementById('download-meta');
  button.href = FALLBACK_URL;

  fetch('https://api.github.com/repos/' + REPO + '/releases/latest', {
    headers: { Accept: 'application/vnd.github+json' }
  })
    .then(function (res) {
      if (!res.ok) throw new Error('HTTP ' + res.status);
      return res.json();
    })
    .then(function (release) {
      var assets = release.assets || [];
      var installer = null;
      for (var i = 0; i < assets.length; i++) {
        if (/\.exe$/i.test(assets[i].name)) {
          installer = assets[i];
          break;
        }
      }
      if (installer) {
        button.href = installer.browser_download_url;
        var sizeMb = (installer.size / (1024 * 1024)).toFixed(0);
        meta.textContent =
          (release.tag_name || 'Latest') + ' · ' + sizeMb + ' MB · Minecraft 1.21.11 · Free';
      }
    })
    .catch(function () {
      /* fallback link already set — nothing to do */
    });
})();
