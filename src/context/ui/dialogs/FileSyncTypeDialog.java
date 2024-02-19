package context.ui.dialogs;

import arc.func.Cons;
import context.ui.elements.CodingTabArea;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;

public class FileSyncTypeDialog {

    public FileSyncTypeDialog(boolean resetOption, boolean optional, Cons<SyncType> cb) {
        BaseDialog bd = new BaseDialog("@choose");

        bd.cont.label(() -> "@context.code-ide.difference");
        bd.buttons.defaults().size(230f,64f);

        if (optional) bd.closeOnBack(() -> cb.get(SyncType.CANCEL));
        if (resetOption) {
            bd.buttons.button("@context.delete", Icon.trash, () -> {
                cb.get(SyncType.DELETE);
                bd.hide();
            }).tooltip("@context.delete.tooltip");
        }
        bd.buttons.button("@context.upload", Icon.upload, () -> {
            cb.get(SyncType.UPLOAD);
            bd.hide();
        }).tooltip("@context.upload.tooltip");
        bd.buttons.button("@context.download", Icon.download, () -> {
            cb.get(SyncType.DOWNLOAD);
            bd.hide();
        }).tooltip("@context.download.tooltip");
        if (optional) {
            bd.buttons.button("@context.cancel", Icon.none, () -> {
                cb.get(SyncType.CANCEL);
                bd.hide();
            }).tooltip("@context.cancel.tooltip");
        }
        bd.show();
    }

    public enum SyncType {
        DELETE, UPLOAD, DOWNLOAD, CANCEL
    }
}
