const template = `
<style>
.installButton {
  position: absolute; bottom: 15px; right: 15px;
  display:inline-block;
  padding:0.7em 1.4em;
  margin:0 0.3em 0.3em 0;
  border-radius:0.15em;
  box-sizing: border-box;
  text-decoration:none;
  font-family:'Roboto',sans-serif;
  text-transform:uppercase;
  font-weight:400;
  background-color:#3369ff;
  color:#FFFFFF;
  box-shadow:inset 0 -0.6em 0 -0.35em rgba(0,0,0,0.17);
  text-align:center;
  border: none;
  width: 100px;
  height: 40px;
}
.loader,
.loader:before,
.loader:after {
  border-radius: 50%;
  width: 2.5em;
  height: 2.5em;
  -webkit-animation-fill-mode: both;
  animation-fill-mode: both;
  -webkit-animation: load7 1.8s infinite ease-in-out;
  animation: load7 1.8s infinite ease-in-out;
}
.loader {
  color: #ffffff;
  font-size: 5px;
  margin: -18px auto;
  position: relative;
  text-indent: -9999em;
  -webkit-transform: translateZ(0);
  -ms-transform: translateZ(0);
  transform: translateZ(0);
  -webkit-animation-delay: -0.16s;
  animation-delay: -0.16s;
}
.loader:before,
.loader:after {
  content: '';
  position: absolute;
  top: 0;
}
.loader:before {
  left: -3.5em;
  -webkit-animation-delay: -0.32s;
  animation-delay: -0.32s;
}
.loader:after {
  left: 3.5em;
}
@-webkit-keyframes load7 {
  0%,
  80%,
  100% {
    box-shadow: 0 2.5em 0 -1.3em;
  }
  40% {
    box-shadow: 0 2.5em 0 0;
  }
}
@keyframes load7 {
  0%,
  80%,
  100% {
    box-shadow: 0 2.5em 0 -1.3em;
  }
  40% {
    box-shadow: 0 2.5em 0 0;
  }
}

.pluginStore li {
  list-style: none;
  border: 1px solid black;
  display: inline-flex;
  background-color: white;
  padding: 15px;
  height: 250px;
  margin: 10px;
  width: 700px;
  position: relative;
  box-shadow: 5px 5px 5px lightgrey;
  overflow: hidden;
}

.pluginStore .successOverlay {
  position: absolute;
  pointer-events: none;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: rgba(51, 255, 83, 0.15);
  padding-top: 100px;
  text-align: center;
  filter: drop-shadow(0 0 0.75rem green);
  opacity: 0;
  transition: opacity 0.2s;
}

.pluginStore .removeOverlay {
  position: absolute;
  pointer-events: none;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: rgba(255, 51, 51, 0.15);
  padding-top: 65px;
  text-align: center;
  // filter: drop-shadow(0 0 0.75rem red);
  opacity: 0;
  transition: opacity 0.2s;
  color: red;
}

.pluginStore .removeOverlay span {
  transform: rotate(-20deg);
  display: inline-block;
  -webkit-text-stroke: 3px darkred;
}

.checkmark {
  display:inline-block;
  width: 22px;
  height:22px;
  transform: scale(15) rotate(45deg);
  filter: drop-shadow(0 0 .4px green);
}

.checkmark div {
  background-color:#29e61a;
}

.checkmark_stem {
  position: absolute;
  width:3px;
  height:9px;
  left:11px;
  top:6px;
}

.checkmark_kick {
  position: absolute;
  width:3px;
  height:3px;
  left:8px;
  top:12px;
}

.stamp {
	font-weight: 700;
	padding: 0.25rem 1rem;
	text-transform: uppercase;
	border-radius: 1rem;
	font-family: 'Courier';
}

.is-nope {
  color: #D23;
  border: 0.5rem double #D23;
	-webkit-mask-position: 2rem 3rem;
  font-size: 6rem;
}

.pluginStore > div > ul {
  transition: margin 1s;
}

.git-fork {
  display: block;
  position: absolute;
  top: 3rem;
  right: -7rem;
  width: 25rem;
  line-height: 2em;
  height: 2em;
  transform: rotate(45deg);
  background-color: #b5152b;
  text-indent: .9em;
}
.git-fork a {
  position: relative;
  width: 100%;
  height: 100%;
  color: #dedede;
  display: block;
  text-align: center;
  text-decoration: none;
}
.git-fork a:hover {
  text-decoration: underline;
}
.git-fork a:before, .git-fork a:after {
  content: '';
  display: block;
  position: absolute;
  top: 0;
  left: 0;
  height: 3px;
  width: 100%;
  border-bottom: 1px dashed #dedede;
}
.git-fork a:after {
  bottom: 0;
  top: auto;
  border-bottom-width: 0;
  border-top: 1px dashed #dedede;
}


</style>
<div class="dashboard pluginStore" style="background-color: #f3f3f3; padding: 0 15em;">
  <h1>Plugin Store</h1>
  <div id="availableContainer">
  </div>
  <div id="installedContainer">
  </div>
</div>
`;

export default [
  {
    id: "cockpit.pluginStore.navigation",
    pluginPoint: "cockpit.navigation",
    properties: {
      path: "/plugins",
    },
    render: (node) => {
      node.innerHTML = `<a href="#/plugins">Plugin Store</a>`;
    },
    priority: 1000,
  },
  {
    id: "cockpit.pluginStore.route",
    pluginPoint: "cockpit.route",
    properties: {
      path: "/plugins",
    },
    render: async (node) => {
      const allPluginsR = await fetch("/store/plugins");
      const allPlugins = await allPluginsR.json();

      const installedReqR = await fetch("/store/installed");
      const installedPlugins = (await installedReqR.json()).map((id) =>
        allPlugins.find((plugin) => plugin.id === id)
      );

      const availablePlugins = allPlugins.filter(
        ({ id }) => !installedPlugins.find((plugin) => plugin.id === id)
      );

      node.innerHTML = template;

      if (availablePlugins.length > 0) {
        let n = node.querySelector("#availableContainer");
        n.innerHTML = `<h2>Available Plugins</h2>
        <ul></ul>`;

        n = n.querySelector("ul");

        availablePlugins.forEach((plugin) => {
          const li = document.createElement("li");
          li.innerHTML = `<div style="display: inline; height: 100%; width: 325px; flex: none; background-size: cover; background-image: url(/store/image/${plugin.id}.png); background-position: center;"></div>
          <!--img src="/store/image/${plugin.id}.png" style="height: 100%;" /-->
          <div style="float: right; margin-left: 15px;">
            <h3>${plugin.title}</h3>
            <p>${plugin.description}</p>
            </div>
          <button class="installButton">Install</button>
          <div class="successOverlay" id="overlay_{{plugin.id}}">
            <span class="checkmark">
              <div class="checkmark_stem"></div>
              <div class="checkmark_kick"></div>
            </span>
          </div>`;

          const btn = li.querySelector("button");
          btn.addEventListener("click", async () => {
            btn.innerHTML = '<div class="loader"></div>';
            btn.style.cursor = "default";
            await fetch("/store/install?id=" + plugin.id);
            li.querySelector(".successOverlay").style.opacity = "1";
            btn.style.opacity = "0";
          });

          n.appendChild(li);
        });
      }

      if (installedPlugins.length > 0) {
        let n = node.querySelector("#installedContainer");
        n.innerHTML = `<h2>Installed Plugins</h2>
        <ul></ul>`;

        n = n.querySelector("ul");

        installedPlugins.forEach((plugin) => {
          const li = document.createElement("li");
          li.innerHTML = `<div style="display: inline; height: 100%; width: 325px; flex: none; background-size: cover; background-image: url(/store/image/${plugin.id}.png); background-position: center;"></div>
          <!--img src="/store/image/${plugin.id}.png" style="height: 100%;" /-->
          <div style="float: right; margin-left: 15px;">
            <h3>${plugin.title}</h3>
            <p>${plugin.description}</p>
            </div>
            <button style="background-color:#ff3333;" class="installButton">Remove</button>
            <div class="removeOverlay" id="overlay_{{plugin.id}}">
              <span class="stamp is-nope">Removed</span>
            </div>`;

          const btn = li.querySelector("button");
          btn.addEventListener("click", async () => {
            btn.innerHTML = '<div class="loader"></div>';
            btn.style.cursor = "default";
            await fetch("/store/uninstall?id=" + plugin.id);
            li.querySelector(".removeOverlay").style.opacity = "1";
            btn.style.opacity = "0";
          });

          n.appendChild(li);
        });
      }
    },
  },
];
