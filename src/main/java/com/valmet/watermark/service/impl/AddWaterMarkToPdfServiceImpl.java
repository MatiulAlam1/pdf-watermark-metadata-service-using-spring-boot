package com.valmet.watermark.service.impl;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.PatternColor;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.PdfPatternCanvas;
import com.itextpdf.kernel.pdf.colorspace.PdfPattern.Tiling;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.layout.LayoutPosition;
import com.itextpdf.layout.properties.Property;
import com.valmet.watermark.config.WatermarkSettings;
import com.valmet.watermark.service.FileDeletionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.lang.Math.PI;

/**
 * AddWaterMarkToPdfServiceImpl for adding watermarks to PDF documents. This
 * class provides methods to create and apply text and logo-based watermarks to
 * existing PDF files, ensuring that metadata and visual elements are preserved.
 *
 * <p>
 * The class uses iText library for PDF manipulation and supports various
 * customization options, such as watermark opacity, font size, and positioning.
 * It also handles exceptions and logs important steps in the watermarking
 * process.
 * </p>
 *
 * <p>
 * <b>Dependencies:</b>
 * </p>
 * <ul>
 * <li>iText PDF library for PDF processing</li>
 * <li>Spring Framework for dependency injection and configuration</li>
 * <li>Apache Commons for utility methods</li>
 * <li>Java AWT for image manipulation</li>
 * </ul>
 *
 * <p>
 * <b>Usage:</b>
 * </p>
 * <ol>
 * <li>Inject this service into a AddWaterMarkToPdfService.</li>
 * <li>Call the `addWatermarkToExistingPdf` method with the necessary input
 * file, output path, and user metadata.</li>
 * </ol>
 *
 * <p>
 * <b>Note:</b>
 * </p>
 * Ensure that the watermark configuration properties are correctly set in the
 * application properties file.
 *
 * @author BJIT
 * @version 1.0
 */

@Service
@Slf4j
public class AddWaterMarkToPdfServiceImpl {
    static Tiling tiling = null;
    private static Image img;
    private final WatermarkSettings watermarkSettings;
    private final FileDeletionService fileDeletionService;
    private ClassPathResource LogoFilePath = null;

    /**
     * Constructor to initialize watermark settings.
     *
     * @param watermarkSettings Watermark configuration settings.
     */
    public AddWaterMarkToPdfServiceImpl (WatermarkSettings watermarkSettings, FileDeletionService fileDeletionService) {
        this.watermarkSettings = watermarkSettings;
        this.fileDeletionService = fileDeletionService;
    }

    /**
     * Applies a watermark to an existing PDF file, including text and logo-based
     * watermarks.
     *
     * @param inputPdf               The path to the input PDF file.
     * @param outputPdf              The path to save the watermarked PDF file.
     * @param strKeyWords            The keywords to include in the PDF metadata.
     * @param mapPdfCustomProperties The custom properties to include in the PDF metadata.
     * @param strWaterMark           The watermark text to apply.
     * @param strWaterMarkImageFile  Path to the watermark image file.
     * @throws IOException If an I/O error occurs during processing.
     */
    public void addWatermarkToExistingPdf (String inputPdf, String outputPdf, String strKeyWords, Map<String, String> mapPdfCustomProperties,
                                           String strWaterMark, String strWaterMarkImageFile) throws IOException {
        log.info ("Starting watermark process for input PDF: {}, output PDF: {}", inputPdf, outputPdf);
        try {
            if (!inputPdf.equalsIgnoreCase (outputPdf)) {
                try (PdfDocument pdfDocument = new PdfDocument (new PdfReader (inputPdf), new PdfWriter (outputPdf))) {
                    PdfDocumentInfo info = pdfDocument.getDocumentInfo ();
                    info.setMoreInfo (mapPdfCustomProperties);
                    info.setKeywords (strKeyWords);
                    Image imgLogoWatermark = loadLogoWatermark ();
                    Document document = new Document (pdfDocument);
                    PdfExtGState transparentGraphicState = new PdfExtGState ().setFillOpacity (0.5f);
                    for (int i = 1; i <= document.getPdfDocument ().getNumberOfPages (); i++) {
                        addWatermarkToExistingPage (document, i, strWaterMark, transparentGraphicState, imgLogoWatermark,
                                strWaterMarkImageFile);
                    }
                }
            }
            log.info ("Watermark process completed successfully for input PDF: {}, output PDF: {}", inputPdf, outputPdf);

        } catch (Exception e) {
            log.error ("Error adding watermark: {}", e.getMessage ());
        } finally {
            log.info ("Finally block in Add Watermark to PDF IMPLEMENTATION");
            fileDeletionService.scheduleFileDeletionIfExists (strWaterMarkImageFile, "Watermark Image");
            fileDeletionService.scheduleFileDeletionIfExists (inputPdf, "Input PDF");
        }
    }

    /**
     * Loads the logo watermark image from the classpath.
     *
     * @return An `Image` object containing the logo watermark.
     * @throws IOException If an I/O error occurs during loading.
     */
    private Image loadLogoWatermark () throws IOException {
        if (LogoFilePath == null) {
            LogoFilePath = new ClassPathResource ("static/images/valmet_logo.png");
        }
        try (InputStream imgInputStream = LogoFilePath.getInputStream ()) {
            ImageData imageData = ImageDataFactory.create (imgInputStream.readAllBytes ());
            Image imgLogoWatermark = new Image (imageData);
            log.info ("Logo Opacity: {}", watermarkSettings.getLogoOpacity ());
            log.info ("Text Opacity: {}", watermarkSettings.getOpacity ());
            imgLogoWatermark.setOpacity (watermarkSettings.getLogoOpacity ());
            return imgLogoWatermark;
        }
    }

    /**
     * Adds a watermark to a specific page in a PDF document.
     *
     * @param document              The iText `Document` representing the PDF.
     * @param pageIndex             The page number (1-based index) to apply the
     *                              watermark.
     * @param strWatermark          The watermark text.
     * @param graphicState          The graphic state for watermark transparency.
     * @param imgLogoWatermark      The image logo watermark.
     * @param strWaterMarkImageFile Path to the watermark image file.
     * @throws IOException If an I/O error occurs during processing.
     */
    private void addWatermarkToExistingPage (Document document, int pageIndex, String strWatermark,
                                             PdfExtGState graphicState, Image imgLogoWatermark, String strWaterMarkImageFile) throws IOException, ExecutionException, InterruptedException {
        PdfDocument pdfDocument = document.getPdfDocument ();
        PdfPage pdfPage = pdfDocument.getPage (pageIndex);
        PageSize pageSize = (PageSize) pdfPage.getPageSizeWithRotation ();
        PdfCanvas over = new PdfCanvas (pdfDocument.getPage (pageIndex));
        over.saveState ();
        over.setExtGState (graphicState);

        float pageX = pageSize.getLeft () + document.getLeftMargin ();
        float pageY = pageSize.getBottom ();
        if (pageIndex == 1) {
            img = null;
            int logoWidth = 450;
            int logoHeight = 400;
            float fontSize = (pageSize.getWidth () + pageSize.getHeight () * 0.8f) / 100;
            if (fontSize >= 45) {
                fontSize = 40;
                logoWidth = 750;
                logoHeight = 700;
            } else if (fontSize >= 40) {
                fontSize = 40;
                logoWidth = 700;
                logoHeight = 650;
            } else if (fontSize >= 35) {
                fontSize = 30;
                logoWidth = 650;
                logoHeight = 600;
            } else if (fontSize >= 30) {
                fontSize = 25;
                logoWidth = 550;
                logoHeight = 500;
            } else if (fontSize >= 20) {
                fontSize = 20;
                logoWidth = 500;
                logoHeight = 450;
            } else if (fontSize <= 13) {
                fontSize = 12;
                logoWidth = 350;
                logoHeight = 300;
            }
            if (!strWatermark.isEmpty ()) {
                img = getWaterMarkedImageByPdfFontSize (strWatermark, (int) fontSize, strWaterMarkImageFile);
            }

            float rotationInRadians = (float) (PI / 180 * 45f);
            imgLogoWatermark.setRotationAngle (rotationInRadians);
            tiling = new Tiling (new Rectangle (logoWidth, logoHeight));
            new Canvas (new PdfPatternCanvas (tiling, pdfDocument), tiling.getBBox ()).add (imgLogoWatermark);
        }
        if (img != null) {
            img.setFixedPosition (pageIndex, pageX + watermarkSettings.getXAxis (), pageY + watermarkSettings.getYAxis ());
            document.add (img);
        }
        new PdfCanvas (pdfPage.newContentStreamAfter (), pdfPage.getResources (), pdfDocument).saveState ()
                .setExtGState (graphicState).setFillColor (new PatternColor (tiling)).rectangle (pdfPage.getCropBox ())
                .fill ().restoreState ();
    }

    public Image getWaterMarkedImageByPdfFontSize (String strWatermark, int fontSize, String strWaterMarkImageFile) {
        try {
            createTextToImage (strWatermark, fontSize, strWaterMarkImageFile);
            ImageData imageData = ImageDataFactory.create (strWaterMarkImageFile);
            Image img = new Image (imageData);
            img.setProperty (Property.POSITION, LayoutPosition.FIXED);
            img.setProperty (Property.FLUSH_ON_DRAW, true);
            return img;
        } catch (IOException e) {
            log.error ("Error creating watermark image: {}", e.getMessage (), e);
        }
        return null;
    }

    /**
     * Creates an image file containing the specified watermark text.
     *
     * @param strWatermark          The watermark text to render on the image.
     * @param fontSize              The font size for the watermark text.
     * @param strWaterMarkImageFile Path to save the generated watermark image.
     */
    private void createTextToImage (String strWatermark, int fontSize, String strWaterMarkImageFile) {
        BufferedImage image = new BufferedImage (3, 3, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2d = image.createGraphics ();
        Font font = new Font (watermarkSettings.getFontName (), getFontStyle (watermarkSettings.getFontStyle ()), fontSize);
        graphics2d.setFont (font);
        FontMetrics fontmetrics = graphics2d.getFontMetrics ();
        int width = fontmetrics.stringWidth (strWatermark);
        int height = fontmetrics.getHeight ();

        graphics2d.dispose ();

        image = new BufferedImage (width + 8, height, BufferedImage.TYPE_INT_ARGB);
        graphics2d = image.createGraphics ();
        graphics2d.setRenderingHint (RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics2d.setRenderingHint (RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        graphics2d.setRenderingHint (RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics2d.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2d.setRenderingHint (RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        graphics2d.setRenderingHint (RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        graphics2d.setRenderingHint (RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics2d.setRenderingHint (RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2d.setRenderingHint (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics2d.setRenderingHint (RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        AlphaComposite alphaChannel = AlphaComposite.getInstance (AlphaComposite.SRC_OVER,
                watermarkSettings.getOpacity ());
        graphics2d.setComposite (alphaChannel);
        graphics2d.setFont (font);
        Color color = Color.decode (watermarkSettings.getColorCode ());
        fontmetrics = graphics2d.getFontMetrics ();
        graphics2d.setColor (color);
        graphics2d.drawString (strWatermark, 0, fontmetrics.getAscent ());
        graphics2d.dispose ();
        try {
            ImageIO.write (image, "PNG", new File (strWaterMarkImageFile));
        } catch (IOException ex) {
            log.error ("Error to write watermark image: {}", ex.getMessage ());
        }
    }

    /**
     * Retrieves the appropriate font style based on the input string.
     *
     * @param style A string representing the font style (e.g., "BOLD", "ITALIC").
     * @return The corresponding integer value for the font style.
     */
    private int getFontStyle (String style) {
        return switch (style.toUpperCase ()) {
            case "LAYOUT_LEFT_TO_RIGHT" -> Font.LAYOUT_LEFT_TO_RIGHT;
            case "BOLD" -> Font.BOLD;
            case "ITALIC" -> Font.ITALIC;
            case "LAYOUT_RIGHT_TO_LEFT" -> Font.LAYOUT_RIGHT_TO_LEFT;
            case "BOLDITALIC", "BOLD_ITALIC" -> Font.BOLD | Font.ITALIC;
            default -> Font.PLAIN;
        };
    }

    /**
     * Generates an image based on the given text, font size, and style.
     *
     * @param strWatermark          The watermark text.
     * @param fontSize              The font size for the watermark.
     * @param strWaterMarkImageFile Path to save the generated image.
     * @return An `Image` object containing the watermark.
     */
    @Async ("taskExecutor")
    public CompletableFuture<Image> getWaterMarkedImageByPdfFontSize_ (String strWatermark, int fontSize, String strWaterMarkImageFile) {
        try {
            createTextToImage (strWatermark, fontSize, strWaterMarkImageFile);
            ImageData imageData = ImageDataFactory.create (strWaterMarkImageFile);
            Image img = new Image (imageData);
            img.setProperty (Property.POSITION, LayoutPosition.FIXED);
            img.setProperty (Property.FLUSH_ON_DRAW, true);
            return CompletableFuture.completedFuture (img);

        } catch (IOException e) {
            log.error ("Error creating watermark image: {}", e.getMessage (), e);
            return CompletableFuture.completedFuture (null);
        } catch (Exception e) {
            log.error ("Unexpected error: {}", e.getMessage (), e);
            return CompletableFuture.completedFuture (null);
        }
    }
}
