/* Axo Client website — shared behavior. Dependency-free, static-host safe. */
(function () {
  'use strict';

  var REPO = 'Poezeloezewoefke1/Axoclient';
  var FALLBACK_URL = 'https://github.com/' + REPO + '/releases/latest';

  /* ---- Community link ----
     Put your real Discord invite here and every [data-discord] element on the
     site turns into a working link. Leave it empty and they stay hidden —
     better a missing button than a dead one on a download page. */
  var DISCORD_INVITE = 'https://discord.gg/nKXpBaeeyy';

  var discordSlots = document.querySelectorAll('[data-discord]');
  for (var d = 0; d < discordSlots.length; d++) {
    var slot = discordSlots[d];
    if (DISCORD_INVITE) {
      slot.setAttribute('href', DISCORD_INVITE);
      slot.setAttribute('rel', 'noopener');
      slot.hidden = false;
    } else {
      slot.hidden = true;
    }
  }
  // Whole sections that exist only to hold a Discord link.
  var discordBands = document.querySelectorAll('[data-discord-band]');
  for (var b = 0; b < discordBands.length; b++) {
    discordBands[b].hidden = !DISCORD_INVITE;
  }

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

  /* ---- Total downloads across every release asset ---- */
  var countEls = document.querySelectorAll('[data-download-count]');
  if (countEls.length) {
    fetch('https://api.github.com/repos/' + REPO + '/releases', {
      headers: { Accept: 'application/vnd.github+json' }
    })
      .then(function (res) {
        if (!res.ok) throw new Error('HTTP ' + res.status);
        return res.json();
      })
      .then(function (releases) {
        // Installers only. Counting every asset would fold in latest.yml and
        // the .blockmap files, which electron-updater fetches on a schedule
        // from every running launcher — the total would be update checks
        // wearing a download's clothes.
        var total = 0;
        releases.forEach(function (r) {
          (r.assets || []).forEach(function (a) {
            if (/\.exe$/i.test(a.name)) {
              total += a.download_count || 0;
            }
          });
        });
        // Nothing published yet reads as broken; say nothing instead.
        if (total > 0) {
          countEls.forEach(function (el) {
            el.textContent = total.toLocaleString() + ' downloads';
            el.hidden = false;
          });
        }
      })
      .catch(function () {
        /* Offline or rate-limited: leave the element hidden. */
      });
  }

  /* ---- Changelog built from published releases ---- */
  var log = document.querySelector('[data-changelog]');
  if (log) {
    fetch('https://api.github.com/repos/' + REPO + '/releases', {
      headers: { Accept: 'application/vnd.github+json' }
    })
      .then(function (res) {
        if (!res.ok) throw new Error('HTTP ' + res.status);
        return res.json();
      })
      .then(function (releases) {
        var published = releases.filter(function (r) {
          return !r.draft;
        });
        if (!published.length) return;

        log.innerHTML = '';
        published.forEach(function (release) {
          var card = document.createElement('div');
          card.className = 'release';

          var head = document.createElement('div');
          head.className = 'release-head';

          var title = document.createElement('h3');
          title.textContent = release.name || release.tag_name || 'Release';
          head.appendChild(title);

          if (release.prerelease) {
            var badge = document.createElement('span');
            badge.className = 'badge badge-soon';
            badge.textContent = 'Beta';
            head.appendChild(badge);
          }
          if (release.published_at) {
            var when = document.createElement('span');
            when.className = 'release-date';
            when.textContent = new Date(release.published_at).toLocaleDateString();
            head.appendChild(when);
          }
          card.appendChild(head);

          var list = document.createElement('ul');
          list.className = 'changelist';
          (release.body || '')
            .split(/\r?\n/)
            .map(function (line) {
              return line.replace(/^\s*[-*+]\s+/, '').replace(/^#+\s*/, '').trim();
            })
            .filter(function (line) {
              return line && !/^\*\*Full Changelog\*\*/i.test(line) && !/^https?:\/\/\S+$/.test(line);
            })
            .forEach(function (line) {
              var li = document.createElement('li');
              li.textContent = line; /* textContent: release bodies are untrusted markdown */
              list.appendChild(li);
            });
          if (list.childNodes.length) card.appendChild(list);
          log.appendChild(card);
        });
      })
      .catch(function () {
        /* Keep the built-in changelog that's already in the HTML. */
      });
  }

  /* ---- Footer year ---- */
  var yr = document.querySelector('[data-year]');
  if (yr) yr.textContent = new Date().getFullYear();
})();
