#include "MqClient.h"  


int main_mqclient(int argc, char* argv[]) {  
	Logger::configDefaultLogger(0, LOG_DEBUG); 
	Logger* log = Logger::getLogger();

	MqClient client("localhost:15555");
	client.connect();
	 
	TrackerInfo info = client.queryTracker();
	log->info("%s", info.serverAddress.address.c_str());

	client.declareTopic("CPP_Topic");

	TopicInfo topicInfo = client.queryTopic("CPP_Topic"); 
	log->info("%s", topicInfo.topicName.c_str());

	ConsumeGroupInfo groupInfo = client.queryGroup("CPP_Topic", "CPP_Topic");
	log->info("%s", groupInfo.groupName.c_str());

	
	ConsumeGroup group;
	group.groupName = "MyCpp";
	group.filter = "abc.*";

	client.declareGroup("CPP_Topic", group);

	client.removeGroup("CPP_Topic", "MyCpp");

	client.removeTopic("CPP_Topic");

	system("pause");
	return 0;
}