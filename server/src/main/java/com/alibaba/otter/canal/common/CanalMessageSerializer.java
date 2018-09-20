package com.alibaba.otter.canal.common;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.CanalPacket;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;
import java.util.List;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.util.CollectionUtils;

public class CanalMessageSerializer {
    public static byte[] serializer(Message data){
        try {
            if (data != null) {
                if (data.getId() != -1) {
                    if (data.isRaw() && !CollectionUtils.isEmpty(data.getRawEntries())) {
                        // for performance
                        List<ByteString> rowEntries = data.getRawEntries();
                        // message size
                        int messageSize = 0;
                        messageSize += CodedOutputStream.computeInt64Size(1, data.getId());

                        int dataSize = 0;
                        for (int i = 0; i < rowEntries.size(); i++) {
                            dataSize += CodedOutputStream.computeBytesSizeNoTag(rowEntries.get(i));
                        }
                        messageSize += dataSize;
                        messageSize += 1 * rowEntries.size();
                        // packet size
                        int size = 0;
                        size += CodedOutputStream.computeEnumSize(3,
                            CanalPacket.PacketType.MESSAGES.getNumber());
                        size += CodedOutputStream.computeTagSize(5)
                            + CodedOutputStream.computeRawVarint32Size(messageSize)
                            + messageSize;
                        // build data
                        byte[] body = new byte[size];
                        CodedOutputStream output = CodedOutputStream.newInstance(body);
                        output.writeEnum(3, CanalPacket.PacketType.MESSAGES.getNumber());

                        output.writeTag(5, WireFormat.WIRETYPE_LENGTH_DELIMITED);
                        output.writeRawVarint32(messageSize);
                        // message
                        output.writeInt64(1, data.getId());
                        for (int i = 0; i < rowEntries.size(); i++) {
                            output.writeBytes(2, rowEntries.get(i));
                        }
                        output.checkNoSpaceLeft();
                        return body;
                    } else if (!CollectionUtils.isEmpty(data.getEntries())) {
                        CanalPacket.Messages.Builder messageBuilder = CanalPacket.Messages.newBuilder();
                        for (CanalEntry.Entry entry : data.getEntries()) {
                            messageBuilder.addMessages(entry.toByteString());
                        }

                        CanalPacket.Packet.Builder packetBuilder = CanalPacket.Packet.newBuilder();
                        packetBuilder.setType(CanalPacket.PacketType.MESSAGES);
                        packetBuilder.setBody(messageBuilder.build().toByteString());
                        return packetBuilder.build().toByteArray();
                    }
                }
            }
        } catch (Exception e) {
            throw new SerializationException("Error when serializing message to byte[] ");
        }
        return null;
    }
}
