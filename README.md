[![Community Extension Badge](https://img.shields.io/badge/Community%20Extension-An%20open%20source%20community%20maintained%20project-FF4700)](https://github.com/camunda-community-hub/community) [![Proof of Concept Badge](https://img.shields.io/badge/Lifecycle-Proof%20of%20Concept-blueviolet)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#proof-of-concept-)

# Welcome to the Camunda Community Hub Plugin Store!

## A project to empower our open source community extension maintainers, plugin authors, and new contributors to get started contributing to the Camunda open source ecosystem.

The Plugin Store is a Camunda Cockpit plugin itself that allows you to install plugins using the Cockpit web interface.

If you want to participate in the Camunda Community Summit 2021 plugin competition, head over to https://github.com/camunda-community-hub/community-summit-plugins and get started writing your own plugin.

---

## How to Install the Plugin Store

### Easy Way

1. Download [this file](https://github.com/camunda-community-hub/camunda-community-pluginstore/blob/main/store.war?raw=true)
2. Put it in the `/server/apache-tomcat-X.X.XX/webapps` directory

### Hard way

Execute the following command to build the plugin store:

```bash
mvn clean install
```

As a result you will find a `store.war` file in the `target` folder.

Copy the .war file to the webapps folder of your shared application
server. For instance, on Tomcat you would copy it to the
`/server/apache-tomcat-X.X.XX/webapps` directory. Tomcat then deploys
the application for you.

---

For more information on how to get started in the Camunda Community Hub, please visit our [community repository](https://github.com/Camunda-Community-Hub/community)!
