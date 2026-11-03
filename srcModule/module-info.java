module Controller{
  requires transitive Coordinator;
  requires java.desktop;
  requires java.net.http;
  exports controller;
  exports gui;
  exports managerMessages;
  exports agentTools;
}