/// How a test in here is written.
///
/// A test is an action list: numbered lines saying what a person does, each one a single statement of the test body, in the same order. An action names no system. Every desk has a background it can show, items on it that open when their icon is clicked, windows that go away and come back, borders that can be dragged, and a bar listing what is open.
///
/// An action never says where any of that is on screen. A desk draws its own decorations and arranges its own icons, so where a border can be grabbed, where the button that sends a window away sits and where an item's icon lands are measured on one desk and written down in the test as one array per desk: one line per action that aims at something, in the order the actions aim. Only those numbers differ between desks. The actions do not, and neither does their count, so an array that comes out a different length elsewhere means the walk went differently there, and that difference is worth reading rather than smoothing over.
///
/// A recording is measured, never derived, and holds for one screen size and one set of items already on the desk. When either changes, or when an action stops landing, the actions are walked again by hand: one gesture at a time, from a process that stays alive for the whole walk, looking at a screen shot after each one and reading the next point off it before sending the next. The numbers are then written down afresh rather than patched, and a desk with no recording at all aborts the test saying so, which is the standing invitation to walk it there.
///
/// A test of a program written for it needs no recording: the program paints everything the test aims at in a colour of its own, and the test reads each aim off a shot taken right before the action, as FearlessGuiTest does.
///
/// So the doc is the test, and the array is only a way of not walking it again while nothing has changed. A recording may be replaced freely; an action may not, because changing one changes what is being tested.
///
/// These tests take the pointer and the keyboard away from everything else on the machine for as long as they run, so they are in no suite that runs by itself: Coordinator/Build/src/scripts/TestAgentTools.java runs them, when someone asks for it.
package agentTools;
