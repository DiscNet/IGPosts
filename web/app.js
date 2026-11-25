(() => {
  "use strict";

  const $ = (selector) => document.querySelector(selector);
  const canvas = $("#preview");
  const ctx = canvas.getContext("2d");
  const state = {
    ratio: "3:4",
    cropToFill: true,
    instagramCrop: true,
    glassChrome: false,
    glassImageBorder: false,
    cardPercent: 80,
    verticalOffset: 0,
    darkness: 39,
    caption: "",
    hashtags: "#graphicdesign #designgrafico #editorialdesign #design",
    selectedProfile: 0,
    profiles: [{ username: "kxlynn666", avatar: null }],
    artwork: null,
    artworkName: "",
  };

  const imageFromData = (data) => new Promise((resolve) => {
    if (!data) return resolve(null);
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => resolve(null);
    image.src = data;
  });

  function roundedRect(x, y, width, height, radius) {
    ctx.beginPath();
    ctx.roundRect(x, y, width, height, radius);
  }

  function drawCover(image, x, y, width, height) {
    const factor = Math.max(width / image.width, height / image.height);
    const sw = width / factor;
    const sh = height / factor;
    const sx = (image.width - sw) / 2;
    const sy = (image.height - sh) / 2;
    ctx.drawImage(image, sx, sy, sw, sh, x, y, width, height);
  }

  function drawContain(image, x, y, width, height) {
    const factor = Math.min(width / image.width, height / image.height);
    const w = image.width * factor;
    const h = image.height * factor;
    ctx.drawImage(image, x + (width - w) / 2, y + (height - h) / 2, w, h);
  }

  function normalizeTags(value) {
    return value.trim().split(/\s+/).filter(Boolean)
      .map((tag) => tag.startsWith("#") ? tag : "#" + tag).join(" ");
  }

  function wrapText(text, maxWidth, font, maxLines = 3) {
    ctx.font = font;
    const words = text.split(/\s+/).filter(Boolean);
    const lines = [];
    let line = "";
    for (const word of words) {
      const candidate = line ? line + " " + word : word;
      if (ctx.measureText(candidate).width <= maxWidth || !line) {
        line = candidate;
      } else {
        lines.push(line);
        line = word;
      }
      if (lines.length === maxLines) break;
    }
    if (line && lines.length < maxLines) lines.push(line);
    if (lines.length === maxLines && words.join(" ").length > lines.join(" ").length) {
      let last = lines[maxLines - 1];
      while (last && ctx.measureText(last + "…").width > maxWidth) last = last.slice(0, -1);
      lines[maxLines - 1] = last + "…";
    }
    return lines;
  }

  function drawBackground() {
    if (state.artwork) {
      ctx.save();
      ctx.filter = "blur(34px) saturate(1.12)";
      drawCover(state.artwork, -45, -45, 1170, 2010);
      ctx.restore();
    } else {
      const gradient = ctx.createLinearGradient(0, 0, 1080, 1920);
      gradient.addColorStop(0, "#e17c57");
      gradient.addColorStop(.3, "#ad897a");
      gradient.addColorStop(.7, "#35343a");
      gradient.addColorStop(1, "#1e2026");
      ctx.fillStyle = gradient;
      ctx.fillRect(0, 0, 1080, 1920);
    }
    ctx.fillStyle = "rgba(17,18,22," + (state.darkness * 255 / 110 / 255) + ")";
    ctx.fillRect(0, 0, 1080, 1920);
    const shade = ctx.createLinearGradient(0, 0, 0, 1920);
    shade.addColorStop(0, "rgba(255,255,255,0)");
    shade.addColorStop(.57, "rgba(0,0,0,0)");
    shade.addColorStop(1, "rgba(0,0,0,.66)");
    ctx.fillStyle = shade;
    ctx.fillRect(0, 0, 1080, 1920);
  }

  function drawGlassSurface(x, y, width, height, upper, scale) {
    const gradient = ctx.createLinearGradient(x, y, x + width, y + height);
    gradient.addColorStop(0, upper ? "rgba(255,255,255,.25)" : "rgba(255,255,255,.09)");
    gradient.addColorStop(1, upper ? "rgba(255,255,255,.06)" : "rgba(255,255,255,.20)");
    ctx.fillStyle = gradient;
    ctx.fillRect(x, y, width, height);
    ctx.strokeStyle = "rgba(255,255,255,.38)";
    ctx.lineWidth = scale;
    ctx.beginPath();
    const lineY = upper ? y + height - scale : y + scale;
    ctx.moveTo(x + 8 * scale, lineY);
    ctx.lineTo(x + width - 8 * scale, lineY);
    ctx.stroke();
  }

  function drawCard() {
    const profile = state.profiles[state.selectedProfile] || state.profiles[0];
    const width = 1080 * state.cardPercent / 100;
    const x = (1080 - width) / 2;
    const imageHeight = state.ratio === "1:1" ? width
      : width * (state.instagramCrop ? 5 / 4 : 4 / 3);
    const scale = width / 864;
    const headerHeight = 104 * scale;
    const pad = 26 * scale;
    const username = (profile.username || "").trim() || "usuario";
    const footer = username + (state.caption.trim() ? "  " + state.caption.trim() : "")
      + (normalizeTags(state.hashtags) ? "  " + normalizeTags(state.hashtags) : "");
    const fontSize = 29 * scale;
    const lines = wrapText(footer, width - pad * 2, fontSize + "px sans-serif", 3);
    const footerHeight = Math.max(120 * scale, lines.length * fontSize * 1.22 + 38 * scale);
    const total = headerHeight + imageHeight + footerHeight;
    const y = Math.max(48, Math.min((1920 - total) / 2 + state.verticalOffset, 1920 - total - 48));
    const radius = 30 * scale;

    ctx.save();
    ctx.shadowColor = "rgba(0,0,0,.12)";
    ctx.shadowBlur = 14 * scale;
    ctx.shadowOffsetY = 3 * scale;
    roundedRect(x, y, width, total, radius);
    ctx.fillStyle = "#fff";
    ctx.fill();
    ctx.restore();

    ctx.save();
    roundedRect(x, y, width, total, radius);
    ctx.clip();

    if (state.glassChrome) drawGlassSurface(x, y, width, headerHeight, true, scale);
    else {
      ctx.fillStyle = "#fff";
      ctx.fillRect(x, y, width, headerHeight);
    }

    const avatarX = x + 57 * scale;
    const avatarY = y + headerHeight / 2;
    const avatarRadius = 31 * scale;
    ctx.save();
    ctx.beginPath();
    ctx.arc(avatarX, avatarY, avatarRadius, 0, Math.PI * 2);
    ctx.clip();
    ctx.fillStyle = "#222329";
    ctx.fillRect(avatarX - avatarRadius, avatarY - avatarRadius, avatarRadius * 2, avatarRadius * 2);
    if (profile.avatar) drawCover(profile.avatar, avatarX - avatarRadius, avatarY - avatarRadius, avatarRadius * 2, avatarRadius * 2);
    else {
      ctx.fillStyle = "#fff";
      ctx.font = "bold " + (avatarRadius * 1.15) + "px sans-serif";
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.fillText("@", avatarX, avatarY);
    }
    ctx.restore();

    ctx.fillStyle = "#212225";
    ctx.font = "bold " + (37 * scale) + "px sans-serif";
    ctx.textBaseline = "alphabetic";
    ctx.textAlign = "left";
    let visible = username;
    while (visible.length && ctx.measureText(visible + "…").width > width - 122 * scale) visible = visible.slice(0, -1);
    ctx.fillText(visible + (visible.length < username.length ? "…" : ""), x + 119 * scale, avatarY + 13 * scale);

    const imageY = y + headerHeight;
    ctx.fillStyle = "#2e3035";
    ctx.fillRect(x, imageY, width, imageHeight);
    if (state.artwork) {
      if (state.cropToFill) drawCover(state.artwork, x, imageY, width, imageHeight);
      else drawContain(state.artwork, x, imageY, width, imageHeight);
    } else {
      const placeholder = ctx.createLinearGradient(x, imageY, x + width, imageY + imageHeight);
      placeholder.addColorStop(0, "#eb815d");
      placeholder.addColorStop(1, "#24252d");
      ctx.fillStyle = placeholder;
      ctx.fillRect(x, imageY, width, imageHeight);
      ctx.strokeStyle = "rgba(255,255,255,.44)";
      ctx.lineWidth = 3 * scale;
      roundedRect(x + 65 * scale, imageY + imageHeight * .2, width - 130 * scale, imageHeight * .6, 15 * scale);
      ctx.stroke();
      ctx.fillStyle = "#fff";
      ctx.textAlign = "center";
      ctx.font = "bold " + (38 * scale) + "px sans-serif";
      ctx.fillText("SUA ARTE AQUI", x + width / 2, imageY + imageHeight / 2);
      ctx.font = (21 * scale) + "px sans-serif";
      ctx.fillText("Toque em IMPORTAR ARTE", x + width / 2, imageY + imageHeight / 2 + 40 * scale);
      ctx.textAlign = "left";
    }

    if (state.glassImageBorder) {
      const border = ctx.createLinearGradient(x, imageY, x + width, imageY + imageHeight);
      border.addColorStop(0, "rgba(255,255,255,.72)");
      border.addColorStop(1, "rgba(255,255,255,.33)");
      ctx.strokeStyle = border;
      ctx.lineWidth = 1.4 * scale;
      ctx.strokeRect(x + 2 * scale, imageY + 2 * scale, width - 4 * scale, imageHeight - 4 * scale);
    }

    const footerY = imageY + imageHeight;
    if (state.glassChrome) drawGlassSurface(x, footerY, width, footerHeight, false, scale);
    else {
      ctx.fillStyle = "#fff";
      ctx.fillRect(x, footerY, width, footerHeight);
    }

    ctx.textBaseline = "top";
    ctx.font = fontSize + "px sans-serif";
    const lineHeight = fontSize * 1.22;
    lines.forEach((line, index) => {
      ctx.fillStyle = "#212225";
      ctx.fillText(line, x + pad, footerY + (footerHeight - lines.length * lineHeight) / 2 + index * lineHeight);
    });

    if (state.glassChrome) {
      ctx.strokeStyle = "rgba(255,255,255,.65)";
      ctx.lineWidth = 1.4 * scale;
      roundedRect(x + scale, y + scale, width - 2 * scale, total - 2 * scale, radius - scale);
      ctx.stroke();
    }
    ctx.restore();
  }

  function render() {
    ctx.clearRect(0, 0, 1080, 1920);
    drawBackground();
    drawCard();
  }

  function updateProfileSelect() {
    const select = $("#profileSelect");
    select.replaceChildren();
    state.profiles.forEach((profile, index) => {
      const option = document.createElement("option");
      option.value = String(index);
      option.textContent = "@" + (profile.username || "usuario");
      select.append(option);
    });
    select.value = String(state.selectedProfile);
    $("#username").value = state.profiles[state.selectedProfile].username;
  }

  function updateControls() {
    document.querySelectorAll("[data-ratio]").forEach((button) => {
      button.classList.toggle("active", button.dataset.ratio === state.ratio);
    });
    document.querySelectorAll("[data-crop]").forEach((button) => {
      button.classList.toggle("active", (button.dataset.crop === "cover") === state.cropToFill);
    });
    $("#instagramCrop").classList.toggle("active", state.instagramCrop);
    $("#fullPortrait").classList.toggle("active", !state.instagramCrop);
    $("#glassCard").classList.toggle("active", state.glassChrome);
    $("#glassArt").classList.toggle("active", state.glassImageBorder);
    $("#glassCard").setAttribute("aria-pressed", String(state.glassChrome));
    $("#glassArt").setAttribute("aria-pressed", String(state.glassImageBorder));
    ["cardPercent", "verticalOffset", "darkness"].forEach((key) => {
      $("#" + key).value = String(state[key]);
      $("#" + key + "Value").value = String(state[key]);
      $("#" + key + "Value").textContent = String(state[key]);
    });
  }

  function persist() {
    try {
      const saved = {
        ...state,
        artwork: null,
        profiles: state.profiles.map((profile) => ({ username: profile.username, avatar: null }))
      };
      localStorage.setItem("igposts-web-settings", JSON.stringify(saved));
    } catch (_) {}
  }

  function bindRange(id, key) {
    $("#" + id).addEventListener("input", (event) => {
      state[key] = Number(event.target.value);
      updateControls();
      render();
      persist();
    });
  }

  async function loadImageFile(file, callback) {
    if (!file || !file.type.startsWith("image/")) return;
    const url = URL.createObjectURL(file);
    const image = new Image();
    image.onload = () => {
      callback(image);
      URL.revokeObjectURL(url);
      render();
    };
    image.onerror = () => URL.revokeObjectURL(url);
    image.src = url;
  }

  $("#artworkInput").addEventListener("change", (event) => {
    const file = event.target.files[0];
    loadImageFile(file, (image) => {
      state.artwork = image;
      state.artworkName = file.name;
      $("#artworkName").textContent = file.name;
    });
  });

  $("#avatarInput").addEventListener("change", (event) => {
    const file = event.target.files[0];
    loadImageFile(file, (image) => {
      state.profiles[state.selectedProfile].avatar = image;
      persist();
    });
  });

  document.querySelectorAll("[data-ratio]").forEach((button) => {
    button.addEventListener("click", () => {
      state.ratio = button.dataset.ratio;
      updateControls(); render(); persist();
    });
  });
  document.querySelectorAll("[data-crop]").forEach((button) => {
    button.addEventListener("click", () => {
      state.cropToFill = button.dataset.crop === "cover";
      updateControls(); render(); persist();
    });
  });

  $("#caption").value = state.caption;
  $("#hashtags").value = state.hashtags;
  $("#caption").addEventListener("input", (event) => { state.caption = event.target.value; render(); persist(); });
  $("#hashtags").addEventListener("input", (event) => { state.hashtags = event.target.value; render(); persist(); });
  $("#username").addEventListener("input", (event) => {
    state.profiles[state.selectedProfile].username = event.target.value.trim().replace(/\s/g, "");
    updateProfileSelect(); render(); persist();
  });
  $("#profileSelect").addEventListener("change", (event) => {
    state.selectedProfile = Number(event.target.value);
    updateProfileSelect(); render(); persist();
  });
  $("#addProfile").addEventListener("click", () => {
    const username = prompt("Nome de usuário do novo perfil:");
    if (username === null) return;
    state.profiles.push({ username: username.trim().replace(/\s/g, ""), avatar: null });
    state.selectedProfile = state.profiles.length - 1;
    updateProfileSelect(); render(); persist();
  });
  $("#removeProfile").addEventListener("click", () => {
    if (state.profiles.length <= 1) return alert("Mantenha pelo menos um perfil.");
    if (!confirm("Remover o perfil selecionado?")) return;
    state.profiles.splice(state.selectedProfile, 1);
    state.selectedProfile = Math.max(0, state.selectedProfile - 1);
    updateProfileSelect(); render(); persist();
  });

  $("#instagramCrop").addEventListener("click", () => { state.instagramCrop = true; updateControls(); render(); persist(); });
  $("#fullPortrait").addEventListener("click", () => { state.instagramCrop = false; updateControls(); render(); persist(); });
  $("#glassCard").addEventListener("click", () => { state.glassChrome = !state.glassChrome; updateControls(); render(); persist(); });
  $("#glassArt").addEventListener("click", () => { state.glassImageBorder = !state.glassImageBorder; updateControls(); render(); persist(); });
  bindRange("cardPercent", "cardPercent");
  bindRange("verticalOffset", "verticalOffset");
  bindRange("darkness", "darkness");

  function exportPng() {
    canvas.toBlob((blob) => {
      if (!blob) return alert("Não foi possível gerar o PNG.");
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "IGPosts-Story.png";
      document.body.append(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    }, "image/png");
  }
  $("#exportButton").addEventListener("click", exportPng);
  $("#exportTop").addEventListener("click", exportPng);

  try {
    const saved = JSON.parse(localStorage.getItem("igposts-web-settings") || "null");
    if (saved && typeof saved === "object") {
      ["ratio", "cropToFill", "instagramCrop", "glassChrome", "glassImageBorder", "cardPercent", "verticalOffset", "darkness", "caption", "hashtags"].forEach((key) => {
        if (saved[key] !== undefined) state[key] = saved[key];
      });
      if (Array.isArray(saved.profiles) && saved.profiles.length) {
        state.profiles = saved.profiles.map((profile) => ({ username: String(profile.username || ""), avatar: null }));
      }
      state.selectedProfile = Math.min(Number(saved.selectedProfile) || 0, state.profiles.length - 1);
    }
  } catch (_) {}

  $("#caption").value = state.caption;
  $("#hashtags").value = state.hashtags;
  updateProfileSelect();
  updateControls();
  render();
})();