package huskybot.commands.moderation

import huskybot.Database
import huskybot.HuskyBot
import huskybot.cmdFramework.*
import huskybot.modules.cmdHelpers.ModHelper
import huskybot.modules.cmdHelpers.Result
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.commands.OptionType

@CommandProperties(description = "Manually add a role to a user.")
class User : Command(ExecutionType.STANDARD){
    override fun execute(ctx: Context) {
        this.subcommands[ctx.event.subcommandName]!!.invoke(ctx)
    }

    @Options([
        Option(name = "user", description = "User whose XP you would like to change", type = OptionType.USER, required = true),
        Option(name = "role", description = "Amount of XP being granted", type = OptionType.INTEGER, required = true)
    ])
    @SubCommand("addrole", "Add a role to a given user", false)
    fun grantXP (ctx: Context) {
        val user = ctx.args.next("user", ArgumentResolver.USER)
        val role = ctx.args.next("role", ArgumentResolver.ROLE)

        /* Null check */
        if (user == null) {
            ctx.post("❌ **Could not find user!** ❌")
            return
        }

        val result = ModHelper.tryAddrole(ctx, ctx.guild?.getMemberById(user.idLong)!!, role!!).get()

        when (result) {
            Result.BOT_NO_PERMS -> ctx.post("❌ **I do not have permissions to change roles!** ❌")
            Result.USER_NO_PERMS -> ctx.post("❌ **You do not have access to this command** ❌")
            Result.MEMBER_TOO_HIGH -> ctx.post("❌ **Cannot add role, <@${user.idLong}> role is above mine!** ❌")
            Result.SUCCESS -> ctx.post("**<@${user.idLong}> has been given <@&${role.idLong}>!**")
            else -> ctx.post("❌ **An error has occurred** ❌")             //This is here to handle any extraneous enum cases.
        }

        ctx.embed{
            setTitle("User Updated")
            setThumbnail(user.avatarUrl)
            addField(MessageEmbed.Field("Role Given:", "<@&${role.idLong}>", true))
        }
    }
}