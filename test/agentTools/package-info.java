/// How a test in here is written.
///
/// A test is an action list: numbered lines saying what a person does, each one a single statement of the test body, in the same order. An action names no system. Every desk has a background it can show, items on it that open when their icon is clicked, windows that go away and come back, borders that can be dragged, and a bar listing what is open.
///
/// An action never says where any of that is on screen. A desk draws its own decorations and arranges its own icons, so where a border can be grabbed, where the button that sends a window away sits and where an item's icon lands are measured on one desk and written down in the test as one aim per action: an aim of a PilotTest, named after what the action does and kept in a field of the same name, holding for each desk where it aims. How long the desk takes to show what an action started is measured the same way: an At holds for each desk the moment, in milliseconds from the start of the test, before which the next action does not start. Only those numbers differ between desks. The actions do not.
///
/// A recording is measured, never derived, and holds for one screen size and one set of items already on the desk. When either changes, or when an action stops landing, the test is walked again in agent mode: TestAgentTools.java given the simple name of the test class runs that test alone, and every aim asks for its numbers instead of reading them. The numbers are then written down afresh rather than patched, and a desk with no recording at all aborts the test saying so, which is the standing invitation to walk it there.
///
/// In agent mode the test talks through the folder out/modular/pilot, which it empties when it starts. An aim asks by leaving there an empty file named after it, and waits until that file holds the answer ended by ';': the coordinates separated by spaces, or for an At the moment in milliseconds since the epoch, where a moment already passed means now. An answer may be written before it is asked for. While an aim waits, a file shot.do holding ';' makes it save a screen shot as shot.png, and hover.do holding 'x y;' moves the pointer there; each .do file disappears when it is done. The test ends as any other, reporting through JUnit.
///
/// A test of a program written for it needs no recording: the program paints everything the test aims at in a colour of its own, and the test reads each aim off a shot taken right before the action, as FearlessGuiTest does.
///
/// So the doc is the test, and the recording is only a way of not walking it again while nothing has changed. A recording may be replaced freely; an action may not, because changing one changes what is being tested.
///
/// These tests take the pointer and the keyboard away from everything else on the machine for as long as they run, so they are in no suite that runs by itself: Coordinator/Build/src/scripts/TestAgentTools.java runs them, when someone asks for it.
package agentTools;
