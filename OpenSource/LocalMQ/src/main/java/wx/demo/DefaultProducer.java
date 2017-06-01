package wx.demo;


import io.openmessaging.KeyValue;
import wx.mq.embedded.client.EmbeddedProducer;
import wx.mq.local.client.LocalProducer;


public class DefaultProducer extends LocalProducer {

    public DefaultProducer(KeyValue properties) {

        super(properties);
    }
}
