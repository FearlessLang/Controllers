package controller;

import java.nio.file.Path;

import fileSupport.NativeLocaleForcer;
import tools.ChildJvm;
import userMessages.UserError;

/// The child JVM the manager starts to compile one project: args are the project
/// folder and the folder where the problem report for Eclipse goes.
public class ChildMain{
  public static void main(String[] args){
    NativeLocaleForcer.forceEnglish();
    ChildJvm.watchParent();
    var project= Path.of(args[0]);
    var reports= Path.of(args[1]);
    Eclipse.problems(reports,"");
    var exitCode= 0;
    var problem= "";
    try{ compile(project); }
    catch(UserError e){ exitCode= 1; problem= e.getMessage(); System.err.println(problem.stripTrailing()); }
    catch(Throwable t){ exitCode= 2; System.err.print(UserError.crash(t)); }
    Eclipse.problems(reports,problem);
    System.out.flush();
    System.err.flush();
    System.exit(exitCode);
  }
  private static void compile(Path project){
    UserError.root= project;
    var c= Session.coordinator(project);
    c.compile(project,c.sourceOracle(Session.stdLib("base")));
  }
}
