package com.staniul;

import com.staniul.teamspeak.ServergroupPresets;
import com.staniul.teamspeak.query.Client;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MttmApplicationTests {
    private static final HashMap<String, String> map = JTS3ServerQuery.parseLine("client_servergroups=12,647,683,696,767,771,928,930,943 client_channel_group_id=97 cid=11704 client_idle_time=2957406 client_unique_identifier=pQFinrbGaonvosDioO0AWZ16DJ8= client_nickname=Filipo client_version=3.1.4\\s[Build:\\s1491993378] client_platform=Windows client_input_muted=0 client_output_muted=0 client_outputonly_muted=0 client_input_hardware=1 client_output_hardware=1 client_default_channel=\\/11411 client_meta_data client_is_recording=0 client_version_sign=rwdyEwnJCzbVfNCqbxMrRyhL5BSYqYSzKQkeZ6m5KImc1F8VB8wEkwwwyxoG7SimC\\/sxIyy4h27CjBFP6rcgBQ== client_security_hash client_login_name client_database_id=13178 client_created=1402872209 client_lastconnected=1497166500 client_totalconnections=2706 client_away=0 client_away_message client_type=0 client_flag_avatar=2a3800c86cb6ee1d9446a8cdcc3eddfa client_talk_power=80 client_talk_request=0 client_talk_request_msg client_description=Śmieszek client_is_talker=0 client_month_bytes_uploaded=0 client_month_bytes_downloaded=424107 client_total_bytes_uploaded=47367522 client_total_bytes_downloaded=135187018 client_is_priority_speaker=0 client_nickname_phonetic=Filipo client_needed_serverquery_view_power=75 client_default_token client_icon_id=0 client_is_channel_commander=1 client_country=PL client_channel_group_inherited_channel_id=11704 client_badges=overwolf=0 client_base64HashClientUID=kfabgcjolgmggkijopkcmaockaonaafjjnhkamjp connection_filetransfer_bandwidth_sent=0 connection_filetransfer_bandwidth_received=0 connection_packets_sent_total=22634 connection_bytes_sent_total=1406734 connection_packets_received_total=23123 connection_bytes_received_total=1434055 connection_bandwidth_sent_last_second_total=81 connection_bandwidth_sent_last_minute_total=111 connection_bandwidth_received_last_second_total=83 connection_bandwidth_received_last_minute_total=104 connection_connected_time=9190636 connection_client_ip=94.254.243.208");
    public static Client client = new Client(18, map);

    @Autowired
    private ServergroupPresets presets;

    @Test
    public void textApplicationContextLoad () throws Exception {
        System.out.println(presets.getHeadAdministrators());
    }
}
