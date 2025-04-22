package huskybot.commands.management

import huskybot.Database
import huskybot.cmdFramework.*
import huskybot.utils.addFields
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.awt.Color

@CommandProperties(
    description = "View and set server specific settings",
    category = CommandCategory.MANAGEMENT
)
class Settings : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        //Invoke subcommand
        this.subcommands[context.event.subcommandName]!!.invoke(context)
    }

    /**
     * Subcommand method that displays the bot's guild-specific settings
     * @param ctx Context object
     */
    @SubCommand("view", "View HuskyBot's settings", false)
    fun view(ctx: Context) {

        /* Get displayable settings */
        val adminRole = ctx.guild?.idLong?.let { Database.getAdminRole(it) }
        val adminRoleFormatted = if (adminRole != null) "<@&${adminRole}>" else "Default: (Admin)"       //Formatted to a Mention

        val modRole = ctx.guild?.idLong?.let { Database.getModRole(it) }
        val modRoleFormatted = if (modRole != null) "<@&${modRole}>" else "Default: (Moderator)"         //Formatted to a Mention

        /* Create array of message embed fields */
        val fields = arrayOf(
            MessageEmbed.Field("Admin Role", adminRoleFormatted, true),
            MessageEmbed.Field("Mod Role", modRoleFormatted, true)
            //MessageEmbed.Field("\u200B", "\u200B", true)
        )

        ctx.embed {
            setTitle("Server Settings | ${ctx.guild?.name}")
            setThumbnail(ctx.guild?.iconUrl)
            setColor(Color.yellow)
            addFields(fields)
        }
    }

    /**
     * Subcommand method that sets the admin role for the bot
     * @param ctx Context object
     */
    @Option("role", "Role that you would like to be set as the admin role, leave blank to clear the role", OptionType.ROLE, false)
    @SubCommand("adminrole","Set the role will be used as the admin role for this bot", false)
    fun adminrole(ctx: Context) {

        val role = ctx.args.next("role", ArgumentResolver.ROLE)

        /* Null Check */
        if (role == null) {
            ctx.guild?.let { Database.setAdminRole(it.idLong, null) }   //Clear the current role from the database

            ctx.post("✅ **Admin Role Cleared** ✅")
            return
        }

        ctx.guild?.let { Database.setAdminRole(it.idLong, role.idLong) }        //Set new admin role in the database

        ctx.embed("✅ Admin Role Set ✅", "**New Role:** <@&${role.idLong}>")
    }

    /**
     * Subcommand method that sets the moderator role for the bot
     * @param ctx Context object
     */
    @Option("role", "Role that you would like to be set as the moderator role, leave blank to clear the role", OptionType.ROLE, false)
    @SubCommand("modrole","Set the role will be used as the moderator role for this bot", false)
    fun modrole(ctx: Context) {

        val role = ctx.args.next("role", ArgumentResolver.ROLE)

        /* Null Check */
        if (role == null) {
            ctx.guild?.let { Database.setModRole(it.idLong, null) }   //Clear the current role from the database

            ctx.post("✅ **Mod Role Cleared** ✅")
            return
        }

        ctx.guild?.let { Database.setModRole(it.idLong, role.idLong) }        //Set new mod role in the database

        ctx.embed("✅ Mod Role Set ✅", "**New Role:** <@&${role.idLong}>")
    }
}