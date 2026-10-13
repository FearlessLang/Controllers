package managerMessages;

import static userMessages.UserError.disp;
import static userMessages.UserError.path;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import controller.Project;
import controller.Registry;
import coordinator.MainsInfo;
import tools.Fs;
import userMessages.UserError;
import utils.Join;

/// Report: what the user gave the manager that it cannot accept: a message, a folder, the
/// content of projects.info, the claims of the mains of a project.
/// The counterpart is Violation, for what the manager can no longer do.
public final class Report{
  public static String quoted(List<String> texts){ return Join.of(texts.stream().map(t->"\""+t+"\""),"",", ",""); }
  public static String fileFailure(IOException e){
    return e.getClass().getSimpleName().replaceFirst("Exception$","").replaceAll("(?<=[a-z])(?=[A-Z])"," ").toLowerCase(Locale.ROOT)+": "+e.getMessage();
  }

  //-- the arguments of the manager program
  public static String tooManyArguments(List<String> args){
    return """
      Fearless was started with %d arguments, but without a first argument starting with
      "FearlessCommand" it takes at most one: the file to open.

      The arguments are:
      %s""".formatted(args.size(),Join.of(args.stream().map(UserError::path),"","\n","",""));
  }
  public static String reservedVerb(String arg){ return "Fearless was started with the argument "+disp(arg)+", but "+disp(arg)+" is not a request: only a running Fearless writes the messages \"InvalidCall\" and \"FatalError\"."; }
  public static String argumentWithNewline(List<String> args){
    return "Fearless was started with an argument holding a newline, but every argument of a FearlessCommand is one line: a path is written as \"Base16:\" then its bytes.\nThe arguments are:\n"+Join.of(args.stream().map(a->"  "+disp(a)),"","\n","","");
  }
  public static String brokenPath(String given){ return "Fearless was started to open "+disp(given)+", but that is not a path of a file this system accepts."; }

  //-- the form of a message
  public static String unreadableMessage(Path file){ return "The manager refused a message, and removed its file: a message is UTF-8 text the manager can read.\nThe bytes of\n"+path(file.toString())+"\ndo not form valid UTF-8 text."; }
  public static String unknownVerb(String verb, List<String> verbs){
    return "The manager was asked "+disp(verb)+", but that is not a request it knows: the requests are "+quoted(verbs)+".";
  }
  public static String wrongLines(String verb, List<String> args, List<Integer> takes){
    return "The manager was asked "+disp(verb)+" with "+args.size()+" argument"+(args.size() == 1 ? "" : "s")+", but "+disp(verb)+" takes "+Join.of(takes.stream().map(i->""+i),""," or ","")+".\nThe arguments are:\n"+Join.of(args.stream().map(a->"  "+disp(a)),"","\n","","  <none>");
  }
  public static String notTagged(String verb, String why){ return "The manager was asked "+disp(verb)+", but its path is not a tagged text: "+why; }
  public static String blankPath(String verb){ return "The manager was asked "+disp(verb)+", but its path is empty."; }
  public static String notAPath(String verb, String given, String why){ return "The manager was asked "+disp(verb)+" on "+disp(given)+", but that is not a path this system accepts: "+why+"."; }
  public static String unknownProject(String verb, String name, List<String> names){
    return "The manager was asked "+disp(verb)+" on the project "+disp(name)+", but no project is named "+disp(name)+"."+Join.of(names.stream().map(n->"\n  "+n),"\nThe projects are:","","","\nNo project is registered.");
  }
  public static String badPort(String text){ return "The manager was given the port "+disp(text)+", but a port is \"Port:\" then a number from 1 to 65535."; }
  public static String portClosed(int port, IOException e){ return "The manager was given the port "+port+", but no program of this machine accepts a connection there: "+fileFailure(e); }

  //-- requests the state of the project refuses
  public static String busy(String verb, String name, String job){ return "The manager was asked "+disp(verb)+" on "+disp(name)+", but "+disp(name)+" has a job: "+disp(job)+". Terminate it first."; }
  public static String notCode(String verb, String name, String kind){ return "The manager was asked "+disp(verb)+" on "+disp(name)+", but "+disp(name)+" is "+disp(kind)+": only a \"code\" project can "+disp(verb)+"."; }
  public static String invalidContent(String verb, String name, String why){ return "The manager was asked "+disp(verb)+" on "+disp(name)+", but its content is invalid:\n"+why.stripTrailing(); }
  public static String notCompiled(String name, String state){ return "The manager was asked \"Run\" on "+disp(name)+", but "+disp(name)+" is "+disp(state)+": compile it first."; }
  public static String noMainNamed(String name, List<String> mains){ return "The manager was asked \"Run\" on "+disp(name)+", but the request names no main."+mains(mains); }
  public static String notAMain(String name, String main, List<String> mains){ return "The manager was asked \"Run\" on "+disp(name)+" for "+disp(main)+", but "+disp(main)+" is not a main of "+disp(name)+"."+mains(mains); }
  private static String mains(List<String> mains){ return Join.of(mains.stream().map(m->"\n  "+m),"\nThe mains are:","","","\n"+"The project has no main."); }
  public static String unknownKind(String name, String text){ return "The manager was asked to change the kind of "+disp(name)+" to "+disp(text)+", but the kinds are \"idle\", \"code\", \"data:readOnly\" and \"data:readWrite\"."; }
  public static String kindChange(String name, String from, String to){ return "The manager was asked to change the kind of "+disp(name)+" from "+disp(from)+" to "+disp(to)+", but a kind changes only from \"idle\", or back to \"idle\"."; }
  public static String malformedLink(String name, String link){ return "The manager was asked to link "+disp(name)+" with "+disp(link)+", but a link is the name of a data project, then \"read\" or \"write\", then the type names the code uses for it, none to remove the link."; }
  public static String linkMissing(String field, String name){ return "\""+field+"\" refers to \""+name+"\", but no project called \""+name+"\" is registered."; }
  public static String linkWrongKind(String field, String name, String kind, boolean needsWrite){
    var needed= needsWrite ? "\"data:readWrite\"" : "\"data:readOnly\" or \"data:readWrite\"";
    return "\""+field+"\" refers to \""+name+"\", but the kind of \""+name+"\" is \""+kind+"\"; \""+field+"\" accepts only "+needed+".";
  }
  public static String linkInvalid(String field, String name, String why){ return "\""+field+"\" refers to \""+name+"\", which is itself invalid:\n"+why; }

  //-- registering a folder
  public static String registerNothing(Path path){ return "The manager was asked to register\n"+path(path.toString())+"\nbut nothing exists there: register an existing folder, or a file inside one."; }
  public static String folderNestedWithRegistered(Path folder, Path registered){
    return """
      Fearless cannot keep track of this project folder.

      The manager was asked to register:
      %s
      Fearless is already keeping track of:
      %s
      One of the two is inside the other. Fearless keeps track of project folders
      that do not overlap, so that every file belongs to exactly one project.

      Use the folder Fearless already keeps track of, or make Fearless forget that
      folder first, and then register this one again.
      """.formatted(path(folder.toString()),path(registered.toString()));
  }
  public static String managerFolderNotAProject(Path folder, Path managerDir){
    return """
      Fearless cannot keep track of this folder as a project.

      The folder is:
      %s
      The manager folder of this Fearless is:
      %s
      The manager folder holds what Fearless remembers about your projects: it is
      never part of a project, and no project is inside it.
      """.formatted(path(folder.toString()),path(managerDir.toString()));
  }
  public static String projectFolderIsRoot(Path root){
    return """
      Fearless cannot keep track of the root of a drive or of the file system as a
      project.

      The folder is:
      %s
      Put the project in a folder inside it, and make that folder the project
      folder.
      """.formatted(path(root.toString()));
  }
  public static String registerRefused(Path folder, IOException e){
    return "The manager was asked to register\n"+path(folder.toString())+"\nbut could not read or change it: "+fileFailure(e)+"\nGive Fearless access to the folder, then register it again.";
  }

  //-- the content of a project folder
  public static String folderMissing(Path folder){ return "The folder of this project does not exist:\n"+path(folder.toString())+"\nRestore it, or forget this project."; }
  public static String folderUnreadable(Path folder, IOException e){ return "The folder of this project can not be read:\n"+path(folder.toString())+"\n"+fileFailure(e)+"\nGive Fearless access to it, or forget this project."; }
  public static String markersMany(Path folder, String marker){ return "More than one .fearless marker file was found in\n"+path(folder.toString())+"\nA project folder holds exactly one, and its name is the project name: keep only \""+marker+"\"."; }
  public static String markerMissing(Path folder, String marker){ return "The marker file \""+marker+"\" is missing from\n"+path(folder.toString())+"\nRestore it, or forget and register this project folder again."; }
  public static UserError projectIconsMany(Path dir, List<Path> found){
    return new UserError("""
      More than one .png file was found for this project's icon.

      Looked in:
      %s

      Found:
      %s

      Keep exactly one .png file there.
      """.formatted(path(dir.toString()),Join.of(found.stream().map(p->"  "+p.getFileName()),"","\n","","")));
  }
  public static UserError projectIconUnreadable(Path icon){
    return new UserError("""
      The icon of this project is not a PNG image Fearless can read.

      File:
      %s

      The icon of a project is the only .png file in its ".config/icon" folder:
      replace that file with a PNG image, or remove it.
      """.formatted(path(icon.toString())));
  }
  public static String noCache(){ return "This project has no compiled cache: compiling it makes one."; }
  public static String stalePackages(List<String> pkgs){ return "The compiled cache does not match the files of the package"+(pkgs.size() == 1 ? " " : "s ")+quoted(pkgs)+": compiling the project compiles "+(pkgs.size() == 1 ? "it" : "them")+"."; }
  public static String packagesToCompile(){ return "The mains of this project are not known: compiling the project makes them known."; }
  public static String resetQuestion(){ return "Reset ends the manager and every job it runs, forgets every registered project and removes the file associations of the manager.\nNothing in the project folders changes."; }
  public static String checkFine(String name){ return "The project "+disp(name)+" is valid, and up to date."; }
  public static String cacheNotDeleted(String why){ return "--- the compiled cache is not deleted: "+why+" ---\n"; }
  public static String notWritten(String file, IOException e){ return "--- "+file+" is not written: "+fileFailure(e)+" ---\n"; }

  //-- projects.info
  public static String infoNotObject(){ return "The whole file must be an object {...} with \"projects\" and \"systemExtensions\"."; }
  public static String infoUnknownKey(String key){ return "Unknown key \""+key+"\": the keys are \"projects\" and \"systemExtensions\"."; }
  public static String infoMissing(String key){ return "The key \""+key+"\" is missing: the keys are \"projects\" and \"systemExtensions\"."; }
  public static String infoProjectsNotObject(){ return "\"projects\" must be an object {...} mapping each project name to the metadata of that project."; }
  public static String infoBadName(String name){ return "\""+name+"\" is not a valid project name: a project name uses only lowercase letters, digits and underscores, starts with a letter or an underscore, and is not a name the file system reserves (\"con\", \"prn\", \"aux\", \"nul\", \"com1\" to \"com9\", \"lpt1\" to \"lpt9\")."; }
  public static String infoOverlap(String name, String other){ return "\""+name+"\" has the same path as \""+other+"\", or one is inside the other; every file belongs to exactly one project."; }
  public static String infoProjectNotObject(String name){ return "The metadata of \""+name+"\" must be an object {...}."; }
  public static String infoUnknownAttribute(String key, List<String> keys){ return "Unknown project attribute \""+key+"\": the attributes of a project are "+quoted(keys)+"."; }
  public static String infoReadAndEdit(String type, String name){ return "\""+type+"\" is in both \"reads\".\""+name+"\" and \"edits\".\""+name+"\": a type name in \"edits\" also reads, so it is not repeated in \"reads\"; a type name in \"reads\" only reads."; }
  public static String infoMissingPath(String name){ return "Project \""+name+"\" is missing its \"path\": the absolute path of the project folder."; }
  public static String infoEmptyPath(){ return "\"path\" cannot be empty: it is the absolute path of the project folder."; }
  public static String infoNotAPath(String why){ return "\"path\" is not a path this system accepts: "+why; }
  public static String infoNotAbsolute(String given){ return "\"path\" must be an absolute path, not \""+given+"\"."; }
  public static String infoMissingKind(String name, String kinds){ return "Project \""+name+"\" is missing its \"kind\": one of "+kinds+"."; }
  public static String infoBadKind(String given, String kinds){ return "\"kind\" must be one of "+kinds+", not \""+given+"\"."; }
  public static String infoNotString(String label){ return label+" must be a string \"...\"."; }
  public static String infoNotList(String label){ return label+" must be a list [...] of strings."; }
  public static String infoNotShape(String given, String label, String shape){ return "\""+given+"\" in "+label+" is not "+shape+"."; }
  public static String infoRepeated(String given, String label){ return "\""+given+"\" is repeated in "+label+"."; }
  public static String infoLinksNotObject(String key){ return "\""+key+"\" must be an object {...} mapping a project name to a list of type names."; }
  public static String infoLinkBadName(String name, String key){ return "\""+name+"\" in \""+key+"\" is not a valid project name."; }
  public static String infoLinkEmpty(String label, String name){ return label+" names no type: a link names the one or more type names the code uses for \""+name+"\"."; }
  public static String metadataNoFile(Path file){ return "The manager was asked to replace projects.info with the content of\n"+path(file.toString())+"\nbut no file is there."; }
  public static String metadataNotUtf8(Path file){ return "The manager was asked to replace projects.info with the content of\n"+path(file.toString())+"\nbut its bytes do not form valid UTF-8 text."; }
  public static String metadataUnreadable(Path file, IOException e){ return "The manager was asked to replace projects.info with the content of\n"+path(file.toString())+"\nbut could not read it: "+fileFailure(e); }
  public static String metadataChanged(){ return "The project metadata is not committed: projects.info changed while it was edited.\nClose the editor, and choose Edit project metadata again to edit what projects.info holds now."; }

  //-- Eclipse
  public static String noEclipse(Path chosen){
    return "Eclipse is not connected: no Eclipse folder, a folder holding the file \".eclipseproduct\", is found from\n"+path(chosen.toString())+"\nSelect the Eclipse folder, a file in it, a file in a folder of it, or a folder holding only the Eclipse folder.";
  }
  public static String eclipseConnected(Path eclipse){ return "Eclipse is connected:\n"+path(eclipse.toString())+"\nRestart Eclipse if it is running: Eclipse loads the Fearless plugin when it starts.\nThen open the Fearless perspective: Window > Perspective > Open Perspective > Other... > Fearless."; }
  public static String eclipseNotConnected(Path chosen, IOException e){ return "Eclipse is not connected from\n"+path(chosen.toString())+"\n"+fileFailure(e); }

  //-- opening a file
  public static String openNoFile(Path file){ return "The manager was asked to open\n"+path(file.toString())+"\nbut that path names no file."; }
  public static String emptyFile(Path file){ return "The manager was asked to open\n"+path(file.toString())+"\nbut that file is empty: no main runs on an empty file."; }
  public static String noMainOpens(Path file, String ext){ return "The manager was asked to open\n"+path(file.toString())+"\nbut no main opens files with the extension "+disp(ext)+"."; }

  //-- the claims of the mains of a project
  public static UserError noFreeExtension(String main, boolean shortcut, String icon){
    var prefix= shortcut ? "fapp" : "ffile";
    var used= "No free extension is left for "+claim(shortcut,icon,"")+" of main "+disp(main)+": all the 1000 extensions \""+prefix+"000\" to \""+prefix+"999\" are used by the projects of this manager.";
    if (shortcut){ return new UserError(used); }
    var name= main.substring(main.lastIndexOf('.')+1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
    var cut= name.substring(0,Math.min(name.length(),Fs.maxExtSeg));
    return new UserError(used+" Claim a system extension instead, for example "+claim(false,icon,Registry.isSystem(cut) ? cut : "ext")+".");
  }
  private static String kind(boolean shortcut){ return shortcut ? "Shortcut" : "OpenWith"; }
  private static String claim(boolean shortcut, String icon, String ext){ return "\"base."+kind(shortcut)+"["+icon+(ext.isEmpty() ? "" : ",\\\""+ext+"\\\"")+"]\""; }
  private static String claim(Project.Claimant c){ return claim(c.shortcut(),c.claim().icon(),c.claim().extension()); }
  public static UserError shortcutNoFileName(String main){
    return new UserError("Main "+disp(main)+" can not have a Shortcut: its name has no matching file name. A main with a Shortcut has a name that can be mapped to a file name, like \"FooBar\" can be mapped to \"foo_bar\".");
  }
  public static UserError shortcutBadFileName(String main, String file){
    return new UserError("Main "+disp(main)+" can not have the shortcut file "+disp(file)+": a file name in a project is at most 200 characters long, and its part before the dot is not \"con\", \"prn\", \"aux\", \"nul\", \"com1\"..\"com9\" or \"lpt1\"..\"lpt9\", reserved on Windows. Give this main another name.");
  }
  public static UserError shortcutMasksFile(String main, String file, String name){
    return new UserError("Main "+disp(main)+" can not have the shortcut file "+disp(file)+": the project has the file "+disp(name)+", and a file without extension can not share its name with a file with an extension. Rename the main, or rename the file "+disp(name)+".");
  }
  public static UserError shortcutsCollide(String main1, String main2, String file){
    return new UserError("Mains "+disp(main1)+" and "+disp(main2)+" can not both have the shortcut file "+disp(file)+": a shortcut file is named by the name of its main and the extension of its Shortcut. Rename one of the two mains, or give one of their Shortcuts another extension.");
  }
  public static String iconGone(Path file){ return "--- the icon file of a claim of this project is gone: "+file+"; the project claims no extension until it is compiled again ---\n"; }
  public static String claimsNotSaved(IOException e){ return "The icons and the extensions of the claims of this project can not be saved in its compiled cache: "+fileFailure(e)+"\nGive Fearless access to the folder, then compile the project again."; }
  public static String extensionsNotAllowed(List<Project.Claimant> cs){
    return Join.of(cs.stream().map(c->"The system extension "+disp(c.claim().extension())+" of "+claim(c)+" of main "+disp(c.main())+" is not in \"systemExtensions\" of projects.info."),"","\n","");
  }
  public static String allowExtensions(List<Project.Claimant> cs){
    return Join.of(cs.stream().map(c->"  "+disp(c.claim().extension())+" for "+claim(c)+" of main "+disp(c.main())+" of project "+disp(c.alias())),
      "Add these system extensions to \"systemExtensions\" of projects.info?\n","\n","\n\nThe manager becomes the program opening every file with these extensions,\nin place of the program opening them now. The project then needs compiling again.");
  }
  public static String claimedBy(List<Project.Claimant> cs){ return Join.of(cs.stream().map(c->disp(c.main())+" of project "+disp(c.alias()))," claimed by "," and ","",""); }
  public static UserError iconRefused(Project.Claimant c, String problem){
    return new UserError(icon(c)+" "+problem+" (from "+from(c.claim())+"): an icon must be a square PNG image with a side from 64 to 1024 pixels.");
  }
  public static UserError iconUnreadable(Project.Claimant c, String why){
    return new UserError(icon(c)+" can not be read from "+from(c.claim())+": "+why+"\nCompile the project again.");
  }
  private static String icon(Project.Claimant c){ return "The icon "+disp(c.claim().icon())+" in "+claim(c)+" of main "+disp(c.main()); }
  private static String from(MainsInfo.Claim c){ return disp(Join.of(Stream.of(c.diskPath(),c.zipSteps().replace(';','/'),c.zipEntry()).filter(s->!s.isEmpty()),"","/","")); }

  //-- what the window opens
  public static String nothingToOpen(Path path){ return "Nothing is opened: nothing exists at\n"+path(path.toString()); }
  public static String notOpened(Path path, IOException e){ return "Nothing is opened at\n"+path(path.toString())+"\n"+fileFailure(e); }
  public static String noDocs(Path gen){ return "The documentation is not opened: no .html file is in\n"+path(gen.toString()); }
  public static String dropRefused(String item, String why){ return "Nothing is registered from "+disp(item)+", dropped on the window: "+why+"."; }
  public static String dropUnreadable(Exception e){ return "Nothing is registered from what was dropped on the window: the desktop did not hand it over: "+e.getMessage(); }
}
