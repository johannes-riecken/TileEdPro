/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tileedpro;


//import org.jdesktop.application.ApplicationActionMap;
//import org.jdesktop.application.ApplicationContext;

/**
 *
 * @author joshua.marinacci@sun.com
 */
public class MacUtil {
    static void setupMac(final TileEdProView vw) {

        /*com.apple.eawt.Application appl = com.apple.eawt.Application.getApplication();
        com.apple.eawt.ApplicationListener appl_list = new com.apple.eawt.ApplicationAdapter() {

            public void handleAbout(com.apple.eawt.ApplicationEvent evt) {
                ApplicationContext context = TileEdProApp.getApplication().getContext();
                ApplicationActionMap map = context.getActionMap(TileEdProView.class, vw);
                Action action = map.get("showAboutBox");
                action.actionPerformed(new ActionEvent(this, -1, "stuff"));
                evt.setHandled(true);
            }

            public void handleQuit(com.apple.eawt.ApplicationEvent evt) {
                ApplicationContext context = TileEdProApp.getApplication().getContext();
                ApplicationActionMap map = context.getActionMap(); //TileEdProView.class, vw);
                for (Object a : map.allKeys()) {
                    System.out.println("aciton = " + a);
                }
                Action action = map.get("quit");
                System.out.println("quit action = " + action);
                action.actionPerformed(new ActionEvent(this, -1, "stuff"));
                evt.setHandled(true);
                evt.setHandled(true);
            }
        };
        appl.addApplicationListener(appl_list);*/
    }

}
