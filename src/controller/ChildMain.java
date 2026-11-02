package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;

import coordinator.Coordinator;
import fileSupport.NativeLocaleForcer;
import tools.Fs;
import tools.PathClassLoader;
import managerMessages.Violation;
import userMessages.Report;
import userMessages.UserError;
import utils.Bug;

/// The child JVM the manager starts to compile one project: the args are the project folder, as PathClassLoader.argOf writes it, and the name of the project.
/// A line starting with takeFreeExtension on the standard output asks the manager for a free extension among the candidates that follow;
/// the manager answers with one line on the standard input, empty when no candidate is free. The end of the standard input halts this JVM.
public final class ChildMain{
  public static final String takeFreeExtension= "FearlessTakeFreeExtension:";
  public static final int cacheCorrupted= 4;
  private static final LinkedBlockingQueue<String> answers= new LinkedBlockingQueue<>();
  public static void main(String[] args){
    NativeLocaleForcer.forceEnglish();
    var t= new Thread(ChildMain::readAnswers, "FearlessParentLifeline");
    t.setDaemon(true);
    t.start();
    var project= PathClassLoader.pathOf(args[0]);
    var exitCode= 0;
    try{
      var error= compile(project, args[1]);
      if (error.isPresent()){ exitCode = UserError.cacheCorrupted(error.get().getMessage()) ? cacheCorrupted : 1; System.err.print(error.get().getMessage().stripTrailing()+"\n"); }
    }
    catch (UserError e){ exitCode = 1; System.err.print(e.getMessage().stripTrailing()+"\n"); }
    catch (VirtualMachineError|LinkageError e){ exitCode = 2; System.err.print(Violation.vmOrLinkageFailure(e).getMessage().stripTrailing()+"\n"); }
    catch (Throwable t2){ exitCode = 3; System.err.print(UserError.crash(t2)); }
    System.out.flush();
    System.err.flush();
    System.exit(exitCode);
  }
  private static Optional<UserError> compile(Path project, String name){
    UserError.root = project;
    Fs.onSymbolicLink= Report::symlinkForbidden;
    return Deployed.coordinator(project).compile(project, Deployed.stdLib("base"), name, ChildMain::take);
  }
  private static Optional<String> take(List<String> candidates){
    System.out.print(takeFreeExtension+String.join(" ", candidates)+"\n");
    System.out.flush();
    try{ var answer= answers.take(); return answer.isEmpty() ? Optional.empty() : Optional.of(answer); }
    catch (InterruptedException e){ throw Bug.of(e); }
  }
  private static void readAnswers(){
    try (var in= new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))){
      for (var line= in.readLine(); line != null; line = in.readLine()){ answers.add(line); }
    }
    catch (IOException _){}
    Runtime.getRuntime().halt(121);
  }
}
