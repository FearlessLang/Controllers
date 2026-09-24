package controller;

import java.nio.file.Path;

import fileSupport.NativeLocaleForcer;
import tools.ChildJvm;
import userMessages.UserError;

/// The child JVM the manager starts to compile one project: the arg is the project folder.
public class ChildMain{
  public static void main(String[] args){
    NativeLocaleForcer.forceEnglish();
    ChildJvm.watchParent();
    var project= Path.of(args[0]);
    var exitCode= 0;
    try{ compile(project); }
    catch(UserError e){ exitCode= 1; System.err.print(e.getMessage().stripTrailing()+"\n"); }
    catch(Throwable t){ exitCode= 2; System.err.print(UserError.crash(t)); }
    System.out.flush();
    System.err.flush();
    System.exit(exitCode);
  }
  private static void compile(Path project){
    UserError.root= project;
    var c= Deployed.coordinator(project);
    c.compile(project,c.sourceOracle(Deployed.stdLib("base")));
  }
}
