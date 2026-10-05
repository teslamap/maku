(function () {
  "use strict";

  var ENDPOINT =
    window.MAKASIA_AI_ENDPOINT ||
    localStorage.getItem("makasia_ai_endpoint") ||
    "https://makasia-ai.sulikoananidze999.workers.dev";

  var style = document.createElement("style");
  style.textContent =
    "#makasiaAIButton{position:fixed;right:20px;bottom:22px;z-index:99999;width:58px;height:58px;border:0;border-radius:18px;background:linear-gradient(135deg,#f3a0bd,#d96895);color:#fff;font-size:25px;box-shadow:0 12px 35px rgba(0,0,0,.25);cursor:pointer}" +
    "#makasiaAI{position:fixed;right:20px;bottom:90px;z-index:99999;width:min(410px,calc(100vw - 24px));height:min(620px,calc(100vh - 110px));display:none;flex-direction:column;overflow:hidden;border-radius:24px;background:#fff;color:#30242a;box-shadow:0 25px 70px rgba(0,0,0,.28);border:1px solid #ead8df}" +
    "#makasiaAI.open{display:flex}.mai-head{display:flex;align-items:center;padding:14px;gap:10px;background:#ffe8f0}.mai-title{font-weight:900}.mai-sub{font-size:11px;opacity:.65}.mai-close{margin-left:auto;border:0;background:transparent;font-size:25px;cursor:pointer}.mai-messages{flex:1;overflow:auto;padding:14px}.mai-msg{max-width:88%;padding:10px 12px;margin:8px 0;border-radius:15px;line-height:1.5;font-size:13px;white-space:pre-wrap}.mai-msg.ai{background:#ffeaf1}.mai-msg.user{margin-left:auto;background:#d96895;color:#fff}.mai-products{margin-top:8px}.mai-product{display:flex;align-items:center;gap:8px;padding:8px;margin-top:7px;border:1px solid #ead8df;border-radius:12px}.mai-product img,.mai-ph{width:48px;height:48px;object-fit:cover;border-radius:10px}.mai-product-info{flex:1}.mai-product-name{font-size:11px;font-weight:800}.mai-product-price{font-size:12px;font-weight:900;margin-top:3px}.mai-product-add{border:0;border-radius:9px;width:32px;height:32px;background:#ffeaf1;color:#d96895;font-size:18px;cursor:pointer}.mai-chips{display:flex;gap:6px;flex-wrap:wrap;padding:8px 12px}.mai-chip{border:1px solid #ead8df;background:#fff;border-radius:20px;padding:7px 10px;font-size:11px;cursor:pointer}.mai-inputbar{display:flex;gap:7px;padding:10px;border-top:1px solid #ead8df}.mai-input{flex:1;min-width:0;height:42px;border:1px solid #ead8df;border-radius:12px;padding:0 12px}.mai-send{width:42px;border:0;border-radius:12px;background:#d96895;color:#fff;font-size:18px;cursor:pointer}@media(max-width:600px){#makasiaAIButton{right:12px;bottom:76px}#makasiaAI{right:8px;bottom:72px;width:calc(100vw - 16px)}}";
  document.head.appendChild(style);

  function getProducts() {
    var source;
    try {
      source = typeof products !== "undefined" ? products : window.products;
    } catch (e) {
      source = window.products;
    }
    if (!Array.isArray(source)) return [];
    return source
      .filter(function (p) { return p && p.status !== "inactive"; })
      .map(function (p) {
        return {
          id: String(p.id || ""),
          name: String(p.name || p.title || "პროდუქტი"),
          category: String(p.category || ""),
          price: Number(p.price) || 0,
          oldPrice: Number(p.oldPrice) || 0,
          stock: Number(p.stock == null ? 0 : p.stock),
          description: String(p.description || ""),
          image: String(p.image || "")
        };
      })
      .filter(function (p) { return p.id && p.name; });
  }

  function esc(value) {
    return String(value == null ? "" : value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function addMessage(role, text, items) {
    var box = document.getElementById("maiMessages");
    if (!box) return;

    var el = document.createElement("div");
    el.className = "mai-msg " + role;
    el.innerHTML = esc(text).replace(/\n/g, "<br>");

    if (role === "ai" && Array.isArray(items) && items.length) {
      var wrap = document.createElement("div");
      wrap.className = "mai-products";

      items.slice(0, 5).forEach(function (p) {
        var card = document.createElement("div");
        card.className = "mai-product";

        var media = p.image
          ? '<img src="' + esc(p.image) + '" alt="' + esc(p.name) + '">'
          : '<div class="mai-ph">🛍️</div>';

        card.innerHTML =
          media +
          '<div class="mai-product-info"><div class="mai-product-name">' +
          esc(p.name) +
          '</div><div class="mai-product-price">' +
          esc(String(p.price) + " ₾") +
          "</div></div>" +
          '<button class="mai-product-add">+</button>';

        card.querySelector(".mai-product-add").onclick = function () {
          if (typeof window.addToCart === "function") {
            window.addToCart(p.id);
            addMessage("ai", "კალათაში დავამატე: " + p.name);
          }
        };

        wrap.appendChild(card);
      });

      el.appendChild(wrap);
    }

    box.appendChild(el);
    box.scrollTop = box.scrollHeight;
  }

  function fallback(question) {
    var ps = getProducts();
    var q = question.toLowerCase();
    var matches = [];

    var numbers = q.match(/\d+(?:[.,]\d+)?/g) || [];
    var budget = numbers.length ? Math.max.apply(null, numbers.map(function (x) {
      return Number(x.replace(",", "."));
    })) : null;

    if (budget !== null && /(ლარ|₾|მდე|ბიუჯეტ|ფას)/.test(q)) {
      matches = ps.filter(function (p) { return p.price <= budget; });
    } else if (/(აქცი|ფასდაკლებ|sale|discount)/.test(q)) {
      matches = ps.filter(function (p) { return p.oldPrice > p.price; });
    } else if (/იაფ/.test(q)) {
      matches = ps.filter(function (p) { return p.stock > 0; })
        .sort(function (a, b) { return a.price - b.price; });
    }

    if (matches.length) {
      return {
        text: "ვიპოვე შესაბამისი პროდუქტები:",
        items: matches.slice(0, 5)
      };
    }

    return {
      text: "AI-სთან დაკავშირება ვერ მოხერხდა. შეგიძლია მკითხო ფასზე, აქციაზე, მარაგზე ან მაგალითად: „50 ლარამდე რა გაქვთ?“",
      items: []
    };
  }


  function ask(question, history) {
    var ps = getProducts();

    return fetch(ENDPOINT, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        message: question,
        history: history.slice(-8),
        products: ps.slice(0, 120)
      })
    })
      .then(function (response) {
        if (!response.ok) throw new Error("AI HTTP " + response.status);
        return response.json();
      })
      .then(function (data) {
        var ids = Array.isArray(data.productIds) ? data.productIds.map(String) : [];
        return {
          text: String(data.answer || "პასუხი ვერ მივიღე."),
          items: ps.filter(function (p) { return ids.indexOf(String(p.id)) !== -1; })
        };
      });
  }

  function openChat() {
    var panel = document.getElementById("makasiaAI");
    if (panel) {
      panel.classList.add("open");
      var input = document.getElementById("maiInput");
      if (input) input.focus();
    }
  }

  function closeChat() {
    var panel = document.getElementById("makasiaAI");
    if (panel) panel.classList.remove("open");
  }

  function sendMessage() {
    var input = document.getElementById("maiInput");
    var question = input.value.trim();
    if (!question) return;

    input.value = "";
    addMessage("user", question);

    var history = window.__makasiaAIHistory || [];
    window.__makasiaAIHistory = history;
    history.push({ role: "user", content: question });

    addMessage("ai", "მოიცადე, ვამოწმებ...");

    var box = document.getElementById("maiMessages");
    var loading = box.lastElementChild;

    ask(question, history)
      .then(function (result) {
        if (loading) loading.remove();
        addMessage("ai", result.text, result.items);
        history.push({ role: "assistant", content: result.text });
      })
      .catch(function (error) {
        console.warn("Makasia AI:", error);
        if (loading) loading.remove();
        var result = fallback(question);
        addMessage("ai", result.text, result.items);
        history.push({ role: "assistant", content: result.text });
      });
  }

  function mount() {
    if (document.getElementById("makasiaAI")) return;

    var button = document.createElement("button");
    button.id = "makasiaAIButton";
    button.type = "button";
    button.textContent = "🤖";
    button.title = "Makasia AI";
    button.onclick = openChat;

    var panel = document.createElement("section");
    panel.id = "makasiaAI";
    panel.innerHTML =
      '<div class="mai-head">' +
      '<div style="font-size:24px">🤖</div>' +
      '<div><div class="mai-title">Makasia AI</div><div class="mai-sub">პროდუქტები • ფასები • აქციები • დახმარება</div></div>' +
      '<button class="mai-close" type="button">×</button>' +
      "</div>" +
      '<div class="mai-messages" id="maiMessages"></div>' +
      '<div class="mai-chips">' +
      '<button class="mai-chip" type="button">50 ₾-მდე რა გაქვთ?</button>' +
      '<button class="mai-chip" type="button">აქციები გაქვთ?</button>' +
      '<button class="mai-chip" type="button">რომელია ყველაზე იაფი?</button>' +
      "</div>" +
      '<form class="mai-inputbar" id="maiForm">' +
      '<input class="mai-input" id="maiInput" autocomplete="off" placeholder="მკითხე რამე...">' +
      '<button class="mai-send" type="submit">➤</button>' +
      "</form>";

    document.body.appendChild(button);
    document.body.appendChild(panel);

    panel.querySelector(".mai-close").onclick = closeChat;

    panel.querySelectorAll(".mai-chip").forEach(function (chip) {
      chip.onclick = function () {
        document.getElementById("maiInput").value = chip.textContent;
        sendMessage();
      };
    });

    document.getElementById("maiForm").onsubmit = function (event) {
      event.preventDefault();
      sendMessage();
    };

    addMessage(
      "ai",
      "გამარჯობა! 👋 მე Makasia-ს AI ასისტენტი ვარ. მკითხე პროდუქტის ფასზე, მარაგზე, აქციებზე ან მითხარი რა ბიუჯეტში ეძებ."
    );
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", mount);
  } else {
    mount();
  }
})();