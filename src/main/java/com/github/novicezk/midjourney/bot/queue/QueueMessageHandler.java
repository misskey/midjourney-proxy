package com.github.novicezk.midjourney.bot.queue;

import com.github.novicezk.midjourney.bot.AdamBotInitializer;
import com.github.novicezk.midjourney.bot.error.ErrorUtil;
import com.github.novicezk.midjourney.bot.utils.Config;
import com.github.novicezk.midjourney.bot.error.ErrorMessageHandler;
import com.github.novicezk.midjourney.bot.utils.ImageDownloader;
import com.github.novicezk.midjourney.enums.MessageType;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.wss.handle.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Slf4j
@Component
public class QueueMessageHandler extends MessageHandler {
    @Override
    public void handle(DiscordInstance instance, MessageType messageType, DataObject message) {
        Guild guild = AdamBotInitializer.getApiInstance().getGuildById(Config.getGuildId());

        // check if there's some issue
        String failReason = ErrorUtil.isError(
                instance, messageType, message, getMessageContent(message), getReferenceMessageId(message));
        if (failReason != null && guild != null) {
            String userId = message.getObject("interaction_metadata").getString("user_id");
            ErrorMessageHandler.sendMessage(
                    guild, userId, "Critical fail! \uD83C\uDFB2\uD83E\uDD26 Try again or upload new image!", failReason);
        }

        // do when task is completed
        if (MessageType.CREATE.equals(messageType) && hasImage(message) && guild != null) {
            TextChannel channel = guild.getTextChannelById(Config.getSendingChannel());
            String userId = getAuthorId(message);
            if (channel != null && userId != null) {
                try {
                    File imageFile = ImageDownloader.downloadImage(getImageUrl(message));
                    FileUpload file = FileUpload.fromData(imageFile);
                    channel.sendMessage("<@" + userId + ">")
                            .addFiles(file)
                            .queue();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
