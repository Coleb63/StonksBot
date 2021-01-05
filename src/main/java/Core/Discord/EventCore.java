package Core.Discord;

import Core.Commands.CommandHandler;
import Core.Commands.CommandResult;
import Core.Configuration.ConfigLoader;
import Core.Permissions.PermissionManager;
import Core.Schedulers.OmxNordic.Model.AttachmentFile;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.guild.member.update.GenericGuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.user.UserTypingEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.util.List;
import java.util.Optional;

/**
 * @author etsubu
 * @version 26 Jul 2018
 *
 */
@Component
public class EventCore extends ListenerAdapter
{
    private static final Logger log = LoggerFactory.getLogger(EventCore.class);
    private final CommandHandler commandHandler;
    private final JDA jda;
    private final PermissionManager permissionManager;
    private final MessageReacter reacter;
    private final FilterHandler filterHandler;

    /**
     * Initializes EventCore
     */
    public EventCore(CommandHandler commandHandler,
                     ConfigLoader configLoader,
                     PermissionManager permissionManager,
                     MessageReacter reacter,
                     FilterHandler filterHandler) throws LoginException {
        this.jda = JDABuilder.createDefault(configLoader.getConfig().getOauth()).build();
        jda.addEventListener(this);
        this.commandHandler = commandHandler;
        this.permissionManager = permissionManager;
        this.reacter = reacter;
        this.filterHandler = filterHandler;
    }

    /**
     * Sends text message to the requested channel on server
     * @param channelList
     * @param message
     * @return
     */
    public boolean sendMessage(List<ServerChannel> channelList, String message, List<AttachmentFile> attachmentFiles) {
        boolean sent = false;
        for(ServerChannel channel : channelList) {
            for(Guild guild : jda.getGuildsByName(channel.getServerName(), true)) {
                for(TextChannel textChannel: guild.getTextChannelsByName(channel.getChannelName(), true)) {
                    MessageAction msg = textChannel.sendMessage(message);
                    if(attachmentFiles != null) {
                        for(AttachmentFile file : attachmentFiles) {
                            msg = msg.addFile(file.getFile(), file.getFilename());
                        }
                    }
                    msg.queue();
                    sent = true;
                }
            }
        }
        return sent;
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        if(event.getAuthor().isBot() || event.getAuthor().getName().equalsIgnoreCase(jda.getSelfUser().getName())) {
            // Skip messages sent by bots or ourselves
            return;
        }
        reacter.react(event).ifPresent(x -> event.getMessage().addReaction(x).queue());
        if(filterHandler.shouldBeFiltered(event)) {
            return;
        }

        if (this.commandHandler.isCommand(event.getMessage().getContentDisplay()) && event.getMessage().getContentDisplay().length() > 1) {
            if(!permissionManager.isReplyAllowed(event)) {
                log.info("Prevented reply for {}  on channel {}", event.getAuthor().getName(), event.getChannel().getName());
                event.getChannel().sendMessage("Please use whitelisted channel for performing commands").queue();
                return;
            }
        	CommandResult result = commandHandler.execute(event);
            event.getChannel().sendMessage(result.getResponse()).queue();
        	if(result.getSucceeded()) {
        	    log.info("Successfully executed user command: {}", event.getMessage().getContentDisplay().replaceAll("\n", ""));
            } else {
        	    log.error("Failed to execute user command: " + result.getResponse() + " - "
                        + Optional.ofNullable(result.getException())
                        .map(Exception::getMessage)
                        .orElse(""));
            }
        }
    }
}