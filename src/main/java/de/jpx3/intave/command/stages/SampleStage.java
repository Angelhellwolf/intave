package de.jpx3.intave.command.stages;

import de.jpx3.intave.command.CommandStage;
import de.jpx3.intave.command.SubCommand;
import de.jpx3.intave.module.Modules;
import de.jpx3.intave.module.nayoro.Nayoro;
import de.jpx3.intave.module.nayoro.event.AttackEvent;
import de.jpx3.intave.module.nayoro.event.EntityRemoveEvent;
import de.jpx3.intave.module.nayoro.event.EntitySpawnEvent;
import de.jpx3.intave.module.nayoro.event.sink.EventSink;
import de.jpx3.intave.user.User;
import org.bukkit.ChatColor;

import java.util.HashSet;
import java.util.Set;

public final class SampleStage extends CommandStage {
  private static SampleStage singletonInstance;

  public SampleStage() {
    super(RootStage.singletonInstance(), "sample");
  }

  @SubCommand(
    selectors = "sinks"
  )
  public void sinksCommand(User user) {
    Nayoro nayoro = Modules.nayoro();
    user.player().sendMessage(ChatColor.GRAY + "活动接收端:");
  }

  @SubCommand(
    selectors = "entitycontrol"
  )
  public void entityControlCommand(User user) {
    Nayoro nayoro = Modules.nayoro();
    nayoro.pushSink(user, new EventSink() {
      private final Set<Integer> entities = new HashSet<>();

      @Override
      public void visit(EntitySpawnEvent event) {
        user.player().sendMessage(ChatColor.GRAY + "生成: " + event.id() + " " + event.size() + " " + event.name());
        entities.add(event.id());
      }

      @Override
      public void visit(EntityRemoveEvent event) {
        user.player().sendMessage(ChatColor.GRAY + "移除: " + event.id());
        if (!entities.remove(event.id())) {
          user.player().sendMessage(ChatColor.RED + "实体 " + event.id() + " 此前未生成！");
        }
      }

      @Override
      public void visit(AttackEvent event) {
        user.player().sendMessage(ChatColor.GRAY + "攻击: " + event.source() + " -> " + event.target());
        if (!entities.contains(event.target())) {
          user.player().sendMessage(ChatColor.RED + "实体 " + event.target() + " 此前未生成！");
        }
      }

      @Override
      public String name() {
        return "EC/anonymous";
      }
    });
    user.player().sendMessage(ChatColor.GREEN + "实体控制已启用");
  }

  public static SampleStage singletonInstance() {
    if (singletonInstance == null) {
      singletonInstance = new SampleStage();
    }
    return singletonInstance;
  }
}
