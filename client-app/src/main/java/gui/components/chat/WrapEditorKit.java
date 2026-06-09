package gui.components.chat;

import javax.swing.text.AbstractDocument;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * EditorKit cho JTextPane cho phép ngắt dòng GIỮA một từ dài (URL, chuỗi không có dấu cách).
 * JTextPane mặc định không bẻ từ dài → tràn ngang; kit này trả minimum span theo trục X = 0
 * để view được phép wrap tại bất kỳ ký tự nào khi không đủ chỗ.
 */
public class WrapEditorKit extends StyledEditorKit {

    private final ViewFactory factory = new WrapColumnFactory();

    @Override
    public ViewFactory getViewFactory() {
        return factory;
    }

    private static class WrapColumnFactory implements ViewFactory {
        @Override
        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                switch (kind) {
                    case AbstractDocument.ContentElementName: return new WrapLabelView(elem);
                    case AbstractDocument.ParagraphElementName: return new ParagraphView(elem);
                    case AbstractDocument.SectionElementName: return new BoxView(elem, View.Y_AXIS);
                    case StyleConstants.ComponentElementName: return new ComponentView(elem);
                    case StyleConstants.IconElementName: return new IconView(elem);
                    default: break;
                }
            }
            return new LabelView(elem);
        }
    }

    /** LabelView cho phép co tới 0 theo trục X ⇒ wrap được giữa từ dài. */
    private static class WrapLabelView extends LabelView {
        WrapLabelView(Element elem) {
            super(elem);
        }

        @Override
        public float getMinimumSpan(int axis) {
            switch (axis) {
                case View.X_AXIS: return 0;
                case View.Y_AXIS: return super.getMinimumSpan(axis);
                default: throw new IllegalArgumentException("Invalid axis: " + axis);
            }
        }
    }
}
