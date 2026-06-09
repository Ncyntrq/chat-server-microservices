package gui.components.chat;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * JPanel cuộn dọc bám đúng bề rộng viewport (không bao giờ cuộn ngang).
 * Nhờ {@code getScrollableTracksViewportWidth()=true}, nội dung con luôn được cấp đúng bề rộng
 * khả dụng ⇒ tin nhắn tự wrap theo viewport thay vì tràn ngang.
 */
public class VerticalScrollablePanel extends JPanel implements Scrollable {

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        // Lấp đầy viewport khi nội dung NGẮN (nền tối phủ kín + glue căn giữa placeholder);
        // khi nội dung CAO hơn viewport → false để dùng preferred height và cuộn được.
        Container parent = getParent();
        if (parent instanceof JViewport) {
            return getPreferredSize().height < parent.getHeight();
        }
        return false;
    }
}
