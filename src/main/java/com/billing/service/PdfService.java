package com.billing.service;

import com.billing.model.Invoice;
import com.billing.model.InvoiceItem;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {
    private final String shopName;

    public PdfService(@Value("${billing.shop-name:My Shop}") String shopName) {
        this.shopName = shopName;
    }

    public byte[] generate(Invoice inv) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font white = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            doc.add(new Paragraph(shopName, title));
            doc.add(new Paragraph("Invoice #" + inv.getId(), bold));
            doc.add(new Paragraph("Date: " + inv.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy")), normal));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Billed to", bold));
            doc.add(new Paragraph(inv.getCustomer().getName(), normal));
            doc.add(new Paragraph(inv.getCustomer().getEmail(), normal));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{5f, 1.2f, 2f, 2f});
            Color head = new Color(15, 107, 92);
            addCell(table, "Item", white, head, Element.ALIGN_LEFT, Rectangle.NO_BORDER);
            addCell(table, "Qty", white, head, Element.ALIGN_RIGHT, Rectangle.NO_BORDER);
            addCell(table, "Price", white, head, Element.ALIGN_RIGHT, Rectangle.NO_BORDER);
            addCell(table, "Total", white, head, Element.ALIGN_RIGHT, Rectangle.NO_BORDER);

            for (InvoiceItem item : inv.getItems()) {
                addCell(table, item.getProductName(), normal, null, Element.ALIGN_LEFT, Rectangle.BOTTOM);
                addCell(table, String.valueOf(item.getQuantity()), normal, null, Element.ALIGN_RIGHT, Rectangle.BOTTOM);
                addCell(table, money(item.getUnitPrice()), normal, null, Element.ALIGN_RIGHT, Rectangle.BOTTOM);
                addCell(table, money(item.getLineTotal()), normal, null, Element.ALIGN_RIGHT, Rectangle.BOTTOM);
            }
            doc.add(table);
            doc.add(new Paragraph(" "));

            PdfPTable totals = new PdfPTable(2);
            totals.setWidthPercentage(40);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            addCell(totals, "Subtotal", normal, null, Element.ALIGN_LEFT, Rectangle.NO_BORDER);
            addCell(totals, money(inv.getSubtotal()), normal, null, Element.ALIGN_RIGHT, Rectangle.NO_BORDER);
            addCell(totals, "Tax", normal, null, Element.ALIGN_LEFT, Rectangle.NO_BORDER);
            addCell(totals, money(inv.getTax()), normal, null, Element.ALIGN_RIGHT, Rectangle.NO_BORDER);
            addCell(totals, "Total", bold, null, Element.ALIGN_LEFT, Rectangle.TOP);
            addCell(totals, money(inv.getTotal()), bold, null, Element.ALIGN_RIGHT, Rectangle.TOP);
            doc.add(totals);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Thank you for your business.", normal));
            doc.close();
        } catch (Exception e) {
            throw new IllegalStateException("Could not create the PDF: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    private static void addCell(PdfPTable table, String text, Font font, Color background,
                                int align, int border) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setBorder(border);
        cell.setPadding(6);
        if (background != null) {
            cell.setBackgroundColor(background);
        }
        table.addCell(cell);
    }

    private static String money(BigDecimal value) {
        return String.format("Rs. %,.2f", value);
    }
}
