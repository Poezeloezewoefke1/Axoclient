/* Axo Client website — shared behavior. Dependency-free, static-host safe. */
(function () {
  'use strict';

  var REPO = 'Poezeloezewoefke1/Axoclient';
  var FALLBACK_URL = 'https://github.com/' + REPO + '/releases/latest';

  /* ---- Mobile nav toggle ---- */
  var toggle = document.querySelector('.nav-toggle');
  var links = document.querySelector('.nav-links');
  if (toggle && links) {
    toggle.addEventListener('click', function () {
      var open = links.classList.toggle('open');
      toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
    });
    links.addEventListener('click', function (e) {
      if (e.target.tagName === 'A') links.classList.remove('open');
    });
  }

  /* ---- Scroll reveal ---- */
  var revealables = document.querySelectorAll('.reveal');
  if ('IntersectionObserver' in window && revealables.length) {
    var io = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) {
            entry.target.classList.add('in');
            io.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: '0px 0px -40px 0px' }
    );
    revealables.forEach(function (el) {
      io.observe(el);
    });
  } else {
    revealables.forEach(function (el) {
      el.classList.add('in');
    });
  }

  /* ---- FAQ accordion ---- */
  document.querySelectorAll('.faq-q').forEach(function (q) {
    q.addEventListener('click', function () {
      var item = q.closest('.faq-item');
      var answer = item.querySelector('.faq-a');
      var isOpen = item.classList.toggle('open');
      answer.style.maxHeight = isOpen ? answer.scrollHeight + 'px' : '0';
      q.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
    });
  });

  /* ---- Live latest-release resolution for download buttons ---- */
  var dlButtons = document.querySelectorAll('[data-download]');
  var metaEls = document.querySelectorAll('[data-release-meta]');
  if (dlButtons.length || metaEls.length) {
    dlButtons.forEach(function (b) {
      b.href = FALLBACK_URL;
    });

    fetch('https://api.github.com/repos/' + REPO + '/releases', {
      headers: { Accept: 'application/vnd.github+json' }
    })
      .then(function (res) {
        if (!res.ok) throw new Error('HTTP ' + res.status);
        return res.json();
      })
      .then(function (releases) {
        // Prefer a launcher release with a Windows installer; fall back to newest.
        var installer = null;
        var chosen = null;
        for (var i = 0; i < releases.length && !installer; i++) {
          var assets = releases[i].assets || [];
          for (var j = 0; j < assets.length; j++) {
            if (/\.exe$/i.test(assets[j].name)) {
              installer = assets[j];
              chosen = releases[i];
              break;
            }
          }
        }
        if (installer) {
          dlButtons.forEach(function (b) {
            b.href = installer.browser_download_url;
          });
          var mb = (installer.size / 1048576).toFixed(0);
          metaEls.forEach(function (m) {
            m.textContent = (chosen.tag_name || 'Latest') + ' · ' + mb + ' MB · Windows';
          });
        } else if (releases[0]) {
          dlButtons.forEach(function (b) {
            b.href = releases[0].html_url;
          });
          metaEls.forEach(function (m) {
            m.textContent = 'Latest builds on GitHub · Windows installer coming with v0.1';
          });
        }
      })
      .catch(function () {
        metaEls.forEach(function (m) {
          m.textContent = 'View releases on GitHub';
        });
      });
  }

  /* ---- Footer year ---- */
  var yr = document.querySelector('[data-year]');
  if (yr) yr.textContent = new Date().getFullYear();
})();
