var zbus = require("../zbus");
var Message = zbus.Message;
var MessageClient = zbus.MessageClient;
var Service = zbus.Service;


var client = new MessageClient("127.0.0.1:15555");
client.connect(function(){
    var service = new Service(client, "MyService");
    service.serve(function(msg){
        console.log(msg.toString());
        var res = new Message();
        res.setBodyFormat("reply from node.js service, {0}", new Date());
        return res;
    });
});


