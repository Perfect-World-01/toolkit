package org.hhq;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.qrcode.EncodeHintType;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author huhaiqing
 */
@SuppressWarnings("all")
public class PdfUtil {

    public boolean pdfToPdf(String from, String to) throws IOException, DocumentException {
        return pdfToPdf(from,null,to,null);
    }
    public boolean pdfToPdf(String from, String to, Map<String,String> fields) throws IOException, DocumentException {
        return pdfToPdf(from,null,to,fields);
    }
    /**
     * pdf转pdf
     * @param from pdf模板路径
     * @param pageRang 选择页的范围，如：1-3(选择第1,2,3页)，1,2(选择第1,2页)，1-2,4-5(选择第1,2,4,5页)
     * @param to   生成的pdf路径
     * @param fields 表单的值映射,key：fileName,value:fileValue
     * @return 返回操作情况
     * @throws IOException
     * @throws DocumentException
     */
    public boolean pdfToPdf(String from, String pageRang, String to, Map<String,String> fields) throws IOException, DocumentException {
        if(from!=null && !from.isEmpty()&& to!=null && !to.isEmpty() && from.endsWith(".pdf")&&to.endsWith(".pdf")){
            PdfReader pdfReader = new PdfReader(from);
            if(pageRang!=null&&!pageRang.isEmpty()) pdfReader.selectPages(pageRang);
            OutputStream outputStream = new FileOutputStream(to);
            PdfStamper pdfStamper = new PdfStamper(pdfReader,outputStream);
            if(fields!=null && fields.size()>0){
                AcroFields acroFields = pdfStamper.getAcroFields();
                for(Map.Entry<String,AcroFields.Item> entry:acroFields.getFields().entrySet()){
                    if(entry.getKey()!=null&&!entry.getKey().isEmpty()&&fields.containsKey(entry.getKey())){
                        acroFields.setField(entry.getKey(),fields.get(entry.getKey()));
                    }
                }
            }
            pdfStamper.setFormFlattening(true);
            outputStream.flush();
            pdfStamper.close();
            outputStream.close();
            pdfReader.close();
            return true;
        }
        return false;
    }

    public boolean pdfToPdf(String from, String pageRang, String to) throws IOException, DocumentException{
        return pdfToPdf(new String[]{from},new String[]{pageRang},to,null,false);
    }
    public boolean pdfToPdf(String from, String pageRang, String to, Map<String,String> fields,boolean before) throws IOException, DocumentException{
        return pdfToPdf(new String[]{from},new String[]{pageRang},to,fields,before);
    }
    public boolean pdfToPdf(String[] froms, String to, Map<String,String> fields,boolean before) throws IOException, DocumentException{
        return pdfToPdf(froms,null,to,fields,before);
    }
    /**
     * pdf转pdf
     * @param froms pdf模板路径数组
     * @param pageRangs 选择页的范围数组，范围支持如：1-3(选择第1,2,3页)，1,2(选择第1,2页)，1-2,4-5(选择第1,2,4,5页)
     * @param to    生成的pdf路径
     * @param fields 表单的值映射,key：fileName,value:fileValue
     * @param before true:合并前将值填充，false:合并后将值填充
     * @return 返回操作情况
     * @throws IOException
     * @throws DocumentException
     */
    public boolean pdfToPdf(String[] froms, String[] pageRangs, String to, Map<String,String> fields,boolean before) throws IOException, DocumentException{
        if(froms!=null&&froms.length>0&&to!=null&&!to.isEmpty()){
            to = to.replaceAll("[\\\\]+","/").replaceAll("/+","/");
            String tempFilePath1 = to.substring(0,to.lastIndexOf("/"))+"/"+new Date().getTime()+".pdf";
            String tempFilePath2 = null;
            OutputStream outputStream = new FileOutputStream(tempFilePath1);
            Document document = new Document();
            PdfCopy pdfCopy = new PdfCopy(document,outputStream);
            document.open();
            for(int i=0;i<froms.length;i++){
                if(froms[i]!=null&&!froms[i].isEmpty()&&froms[i].endsWith(".pdf")){
                    if(before){
                        tempFilePath2 = to.substring(0,to.lastIndexOf("/"))+"/"+new Date().getTime()+".pdf";
                        if(pageRangs!=null&&pageRangs.length>i&&pageRangs[i]!=null) pdfToPdf(froms[i],pageRangs[i],tempFilePath2,fields);
                        else pdfToPdf(froms[i],null,tempFilePath2,fields);
                        froms[i]=tempFilePath2;
                    }
                    PdfReader pdfReader = new PdfReader(froms[i]);
                    String pageRang = null;
                    int maxPages = pdfReader.getNumberOfPages();
                    if(pageRangs!=null&&pageRangs.length>i&&(pageRang=pageRangs[i])!=null);
                    if (pageRang==null || pageRang.isEmpty()) pageRang = String.format("%1$d-%2$d",1,maxPages);
                    List<Integer> list = SequenceList.expand(pageRang,maxPages);
                    for(int j=0;j<list.size();j++){
                        pdfCopy.addPage(pdfCopy.getImportedPage(pdfReader,list.get(j)));
                    }
                    //拷贝表单
                    if (!before) pdfCopy.copyAcroForm(pdfReader);
                    pdfReader.close();
                    //删除多余的文件
                    if(tempFilePath2!=null&&!tempFilePath2.isEmpty()) new File(tempFilePath2).delete();
                }
            }
            outputStream.flush();
            document.close();
            pdfCopy.close();
            outputStream.close();
            if(!before){
                pdfToPdf(tempFilePath1,to,fields);
            }else{
                pdfToPdf(tempFilePath1,to);
            }
            //删除多余的文件
            if(!tempFilePath1.isEmpty()) new File(tempFilePath1).delete();
            return true;
        }
        return false;
    }

    /**
     * html转pdf
     * 要求html文本符合规范,如：字体(宋体->SimSun),任何html元素都需要闭合
     * @param from html存储路径
     * @param to pdf存储路径
     * @return 返回操作情况
     * @throws IOException
     * @throws DocumentException
     */
    public boolean htmlToPdf(String from, String to) throws IOException, DocumentException {
        if(from!=null&&!from.isEmpty()&&to!=null&&!to.isEmpty()){
            Document document = new Document();
            OutputStream outputStream = new FileOutputStream(to);
            InputStream inputStream = new FileInputStream(from);
            PdfWriter pdfWriter = PdfWriter.getInstance(document,outputStream);
            document.open();
            XMLWorkerHelper.getInstance().parseXHtml(pdfWriter,document,inputStream);
            document.close();
            return true;
        }
        return false;
    }

    /**
     * pdf存储路径，只用于自定义方式
     */
    private String pdfPath = "";

    /**
     * 自定义pdf内容
     * @param productPdf 内容操作
     * @return 返回存储路径
     * @throws IOException
     * @throws DocumentException
     */
    public String definePdf(ProductPdf<? super Document,? super PdfWriter> productPdf) throws IOException, DocumentException {
        Document document = new Document();
        OutputStream outputStream = new FileOutputStream(getPdfPath());
        PdfWriter pdfWriter = PdfWriter.getInstance(document,outputStream);
        productPdf.definePdf(document,pdfWriter);
        if(document.isOpen()){
            outputStream.flush();
            document.close();
            outputStream.close();
            pdfWriter.close();
        }
        return pdfPath;
    }
    /**
     * 创建二维码
     * @param contenxt 设置为二维码的内容
     * @param markMask 二维码中心图片
     * @param width 生成的二维码宽
     * @param height 生成的二维码高
     * @param hints 设置二维码参数，如：key:EncodeHintType.ERROR_CORRECTION,value:ErrorCorrectionLevel.H 错误级别;key:EncodeHintType.CHARACTER_SET,value:"ISO-8859-1"(默认);字符类型
     * @return 二维码图片
     * @throws DocumentException
     * @throws IOException
     */
    public Image createQRCode(String contenxt, String markMask, int width, int height, Map<EncodeHintType,Object> hints) throws DocumentException, IOException {
        BarcodeQRCode qrcode = new BarcodeQRCode(contenxt.trim(), width<=0?1:width, height<=0?1:height, hints);
        Image image = qrcode.getImage();
        if(markMask!=null && !markMask.isEmpty()){
            String maskPath = this.getClass().getResource(markMask).getPath();
            Image markMasks = Image.getInstance(maskPath);
            markMasks.makeMask();
            image.setImageMask(markMasks);
        }
        return image;
    }

    /**
     * 创建条形码
     * @param contenxt 编码内容
     * @param altText 提示内容
     * @param pdfContentByte 图层
     * @param baseColor 线条颜色
     * @param textColor 文本颜色
     * @param absoluteX 绝对x坐标
     * @param absoluteY 绝对y坐标
     * @param scale 缩放比例
     * @return 条形码图片
     */
    public Image createBarcode(String contenxt,String altText,PdfContentByte pdfContentByte,BaseColor baseColor,BaseColor textColor,int absoluteX, int absoluteY,int scale){
        Barcode128 code128 = new Barcode128();
        code128.setCode(contenxt.trim());
        code128.setCodeType(Barcode128.CODE128);
        if(altText!=null && !altText.isEmpty()) code128.setAltText(altText);
        Image code128Image = code128.createImageWithBarcode(pdfContentByte, baseColor, textColor);
        code128Image.setAbsolutePosition(absoluteX,absoluteY);
        code128Image.scalePercent(scale);
        return code128Image;
    }

    /**
     * 通过名称获取字体
     * @param name 字体名称
     * @param fontPath 字体存储路径
     * @param family 是否查找字体家族
     * @return 返回查找结果,null:未找到
     */
    public Font defineFont(String name,String fontPath,boolean family){
        FontFactoryImp fontFactory = new FontFactoryImp();
        if(fontPath==null||fontPath.isEmpty()) fontPath="/font";
        fontFactory.registerDirectory(this.getClass().getResource(fontPath).getPath());
        //筛选
        Predicate<String> filter = font -> font!=null && !font.isEmpty() && font.matches(name);
        //获取最长
        Comparator<String> comparator = (str1,str2)->{
            if(str1!=null && !str1.isEmpty() && str2!=null && !str2.isEmpty()){
                if(str1.length()>str2.length()) return -1;
                if(str1.length()<str2.length()) return 1;
            }
            return 0;
        };
        //查找字体
        String fontName;
        if (family)
            fontName = fontFactory.getRegisteredFamilies().stream().filter(filter).min(comparator).get();
        else
            fontName = fontFactory.getRegisteredFonts().stream().filter(filter).min(comparator).get();

        if(fontName==null || fontName.isEmpty()){
            return null;
        }
        return fontFactory.getFont(fontName);
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public PdfPTable getPdfTable(ProductTable productTable) throws IOException, DocumentException {
        return productTable.defineTable();
    }
    public PdfPCell getPdfCell(ProductTableCell productTableCell) throws IOException, DocumentException {
        return productTableCell.defineTableCell();
    }

    /**
     * 存储常用字体
     */
    private Map<String,Font> fontMaps = new HashMap<>();

    public Map<String, Font> getFontMaps() {
        return fontMaps;
    }

    public void setFontMaps(Map<String, Font> fontMaps) {
        this.fontMaps = fontMaps;
    }

    @FunctionalInterface
    interface ProductPdf<D,W>{
        void definePdf(D d,W w) throws IOException, DocumentException;
    }

    @FunctionalInterface
    interface ProductTable{
        PdfPTable defineTable() throws IOException, DocumentException;
    }

    @FunctionalInterface
    interface ProductTableCell{
        PdfPCell defineTableCell() throws IOException, DocumentException;
    }

    //例子
    public static void main(String[] args) throws IOException, DocumentException {
        String bd = "D:\\bd.jpg";
        PdfUtil pdfUtil = new PdfUtil();
        pdfUtil.setPdfPath("D:/pdf/"+new Date().getTime()+".pdf");
        //凡是.ttc结尾的字体后面需要加上,n (n->[0,1])
        BaseFont baseFont = BaseFont.createFont("config/font/华康少女文字简W5.ttc,1",BaseFont.IDENTITY_H,BaseFont.NOT_EMBEDDED);
        Font font = new Font(baseFont,12,Font.BOLD);
        font.setSize(24);
        pdfUtil.definePdf((document,writer)->{
            writer.setPageEvent(new PdfPageEvent(document.getPageSize().getHeight(),document.getPageSize().getWidth(),baseFont));
            writer.setPageEvent(new PefContentByteDemo());
            document.open();
            document.add(new Paragraph("hello world",font));
            document.newPage();
            document.add(new Chunk("title1").setLocalDestination("title1"));
            document.newPage();
            document.add(new Chunk("title2").setLocalDestination("title2"));
            document.newPage();
            document.add(new Chunk("title3").setLocalDestination("title3"));
            document.add(new Paragraph("\n"));
            document.add(new Chunk("title3.1").setLocalDestination("title3.1"));
            document.add(new Paragraph("\n"));
            document.add(new Chunk("title3.2").setLocalDestination("3.2"));
            document.add(new Paragraph("\n"));
            document.add(new Chunk("title3.3").setLocalDestination("3.3"));
            document.add(new Paragraph("\n"));
            document.add(new Chunk("title3.4").setLocalDestination("3.4"));
            document.add(new Paragraph("\n"));

            Anchor anchor = new Anchor();
            anchor.add("demo");
            anchor.setName("test");
            anchor.setReference("www.baidu.com");
            Font anchorFont = new Font(baseFont,18,Font.NORMAL);
            anchorFont.setColor(BaseColor.RED);
            anchor.setFont(anchorFont);
            document.add(anchor);
            document.newPage();
            Anchor anchor2 = new Anchor();
            anchor2.add("go to demo");
            anchor2.setReference("#test");
            anchor2.setFont(anchorFont);
            document.add(anchor2);

            document.add(pdfUtil.getPdfTable(()->{
                PdfPTable pdfPTable = new PdfPTable(1);
                pdfPTable.addCell(pdfUtil.getPdfCell(()->{
                    PdfPCell pdfPCell = new PdfPCell();
                    pdfPCell.addElement(new Chunk("测试1").setLocalDestination("test1"));
                    return pdfPCell;
                }));
                return pdfPTable;
            }));

            document.newPage();

            document.add(chunkDemo());

            document.newPage();

            document.add(tableDemo());

            document.close();
        });

        System.out.println("pdfPath:"+pdfUtil.getPdfPath());
    }
    //例子
    private static Anchor anchorDemo(){
        Anchor anchor = new Anchor();
        //anchor显示的文本内容
        anchor.add("demo");
        //anchor名称
        anchor.setName("test");
        //跳转到指定位置，可以是url，可以是achor名称（#achorName）
        anchor.setReference("www.baidu.com");
        return anchor;
    }
    //例子
    private static Chunk chunkDemo() throws IOException, DocumentException {
        BaseFont baseFont = BaseFont.createFont("config/font/华康少女文字简W5.ttc,1",BaseFont.IDENTITY_H,BaseFont.NOT_EMBEDDED);
        Font font = new Font(baseFont,12,Font.BOLD);
        Chunk chunk = new Chunk();
        //设置字体
        chunk.setFont(font);
        //设置背景颜色
        chunk.setBackground(BaseColor.DARK_GRAY);
        //文本上调2个单位
        chunk.setTextRise(2);
        //添加内容
        chunk.append("apend->ChunkDemo");
        //添加下划线，线宽1，位置：文本底相接为0
        chunk.setUnderline(1,-1);
        //一个跳转链接，其实大部分Element元素最终都是转换为chunk
        chunk.setAnchor("www.baidu.com");

        //该chunk的定位名称
        chunk.setLocalDestination("chunkDemo");
        //该chunk的动作 一般用于跳转到定位点
//        chunk.setAction()
        return chunk;
    }
    //例子
    private static PdfPTable tableDemo() throws IOException, DocumentException {
        PdfPTable pdfPTable = new PdfPTable(2);
        pdfPTable.setTotalWidth(300);
        pdfPTable.setLockedWidth(true);
        pdfPTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        //设置table头
        PdfPHeaderCell pdfPHeaderCell = new PdfPHeaderCell();
        pdfPHeaderCell.setColspan(2);
        pdfPHeaderCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        pdfPHeaderCell.setBackgroundColor(BaseColor.CYAN);
        pdfPHeaderCell.setBorder(2);
        pdfPHeaderCell.setBorder(Rectangle.BOTTOM);
        pdfPHeaderCell.addElement(paragraphDemo("pdfPHeaderCell->paragraph demo"));
        pdfPTable.addCell(pdfPHeaderCell);
        pdfPTable.setHeaderRows(1);
        //设置table body
        PdfPCell pdfPCell = new PdfPCell();
        pdfPCell.setBackgroundColor(BaseColor.RED);
        pdfPCell.addElement(paragraphDemo("pdfPCell->paragraph demo"));
        pdfPCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        pdfPTable.addCell(pdfPCell);

        PdfPCell pdfPCell2 = new PdfPCell();
        pdfPCell2.setBackgroundColor(BaseColor.RED);
        pdfPCell2.addElement(paragraphDemo("pdfPCell->paragraph demo2"));
        pdfPCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
        pdfPTable.addCell(pdfPCell2);
        //设置table无边框，不代表cell无边框，，，
        pdfPTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        return pdfPTable;
    }
    //例子
    private static Paragraph paragraphDemo(String text) throws IOException, DocumentException {
        BaseFont baseFont = BaseFont.createFont("config/font/华康少女文字简W5.ttc,1",BaseFont.IDENTITY_H,BaseFont.NOT_EMBEDDED);
        Font font = new Font(baseFont,12,Font.BOLD);
        Paragraph paragraph = new Paragraph();
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setFont(font);
        paragraph.add(text);
        paragraph.add(new Chunk().setLocalGoto(""));
        return paragraph;
    }
}
//例子
class PdfPageEvent extends PdfPageEventHelper{

    private PdfTemplate pdfTemplate;

    private float pageHeight = 0;

    private float pageWidth = 0;

    private BaseFont baseFont = null;

    public PdfPageEvent(float pageHeight,float pageWidth,BaseFont baseFont){
        this.pageHeight = pageHeight;
        this.pageWidth = pageWidth;
        this.baseFont = baseFont;
    }

    public void onOpenDocument(PdfWriter writer, Document document) {
        super.onOpenDocument(writer, document);
        //设置边距
        document.setMargins(document.leftMargin(),document.rightMargin(),0,document.bottom());
        //创建pdf模板，相当于占位符，用于设置总页数
        PdfContentByte pdfContentByte = writer.getDirectContent();
        pdfTemplate = pdfContentByte.createTemplate(pageWidth,pageHeight);
//        pdfTemplate.setBoundingBox(new Rectangle(0,0,100,100));
        //添加pdf文档头部
        //对应于  writer.getInfo();
        document.addHeader("test","test->show");
        document.addProducer();
        document.addTitle("title");
        document.addAuthor("none");
        document.addCreator("none");
        document.addKeywords("keywords");
        document.addSubject("subject");
        document.addCreationDate();
        document.addLanguage("chinese");
//        document.setJavaScript_onLoad("this is javascript...");
//        document.setJavaScript_onUnLoad("pdf load done");
    }
    public void onStartPage(PdfWriter writer, Document document) {
        super.onStartPage(writer, document);
        try {
            //添加页眉
            if(document.getPageNumber()<=1){
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                Font dateFont = new Font(baseFont,12,Font.BOLD);
                //日期
                Paragraph date = new Paragraph(format.format(new Date()),dateFont);
                date.setAlignment(Element.ALIGN_LEFT);
                //标题
                Font titleFont = new Font(baseFont,12,Font.BOLD);
                Paragraph title = new Paragraph("title",titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                //小标题
                Font smallTitleFont = new Font(baseFont,9,Font.BOLD);
                Paragraph smallTitle = new Paragraph("smallTitle",smallTitleFont);
                smallTitle.setAlignment(Element.ALIGN_CENTER);
                //编号
                Font numberFont = new Font(baseFont,10,Font.BOLD);
                Paragraph number = new Paragraph("number",numberFont);
                number.setAlignment(Element.ALIGN_LEFT);
                //将内容导入document中
                document.add(date);
                document.add(title);
                document.add(smallTitle);
                document.add(number);
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    public void onEndPage(PdfWriter writer, Document document) {
        super.onEndPage(writer, document);
        //添加页标注
        //第()页，共()页
        PdfContentByte pdfContentByte = writer.getDirectContent();
        pdfContentByte.setFontAndSize(baseFont,12);
        pdfContentByte.beginText();
        pdfContentByte.showTextAligned(PdfContentByte.ALIGN_CENTER,String.format("第 %d 页， %s",document.getPageNumber(),pdfTemplate.toString()),(document.getPageSize().getRight()-document.getPageSize().getLeft())/2,document.getPageSize().getBottom()+10,0);
        pdfContentByte.endText();
        pdfContentByte.addTemplate(pdfTemplate,(document.getPageSize().getRight()-document.getPageSize().getLeft())/2,document.getPageSize().getBottom()+10);
    }

    public void onCloseDocument(PdfWriter writer, Document document) {
        super.onCloseDocument(writer, document);
        //设置该模板显示的内容
        pdfTemplate.beginText();
        pdfTemplate.setFontAndSize(baseFont, 12);
        pdfTemplate.setTextMatrix(20, 0);
        pdfTemplate.showText(String.format("共 %s 页",String.valueOf(writer.getPageNumber() - 1)));
        pdfTemplate.endText();

        settingOutline(writer,document);
    }

    public void onParagraph(PdfWriter writer, Document document, float paragraphPosition) {
        super.onParagraph(writer, document, paragraphPosition);
    }

    public void onParagraphEnd(PdfWriter writer, Document document, float paragraphPosition) {
        super.onParagraphEnd(writer, document, paragraphPosition);
    }

    private void settingOutline(PdfWriter writer,Document document){
        //设置文档大纲
        PdfOutline rootOutline = writer.getDirectContent().getRootOutline();
        rootOutline.setOpen(false);
        PdfOutline title1 = new PdfOutline(rootOutline,PdfAction.gotoLocalPage("title1",true),"title1");
        PdfOutline title2 = new PdfOutline(rootOutline,PdfAction.gotoLocalPage("title2",true),"title2");
        PdfOutline title3 = new PdfOutline(rootOutline,PdfAction.gotoLocalPage("title3",true),"title3");
        PdfOutline title31 = new PdfOutline(title3,PdfAction.gotoLocalPage("title3.1",true),"title3.1");
        //PdfOutline：第一个参数：上级目录节点，第二个参数：动作，第三个参数：该位置显示的名称
        //PdfAction：第一个参数:目的地，第二个参数：目的地是否为名称
        PdfOutline title32 = new PdfOutline(title31,PdfAction.gotoLocalPage("3.2",false),"title3.2");
        title3.setOpen(false);
        PdfOutline title33 = new PdfOutline(title3,PdfAction.gotoLocalPage("3.3",false),"3.3");
        writer.setOpenAction("3.4");
    }
}
//例子
class PefContentByteDemo extends PdfPageEventHelper{
    public void onEndPage(PdfWriter writer, Document document) {
        //要求取当前页画布
        //第一层，一般用于扩展正文
        //writer.getDirectContent();
        //第四层，一般用于画水印，背景图等
        //writer.getDirectContentUnder();
        try {
            //画矩形例举
            drawRectangle(writer,document);
            //画文本例举
            drawText(writer,document);
            //画图片
            drawImage(writer,document);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    private void drawRectangle(PdfWriter writer, Document document){

        PdfContentByte pdfContentByte = writer.getDirectContent();
        /**
         * @see PdfContentByte#variableRectangle
         * @see PdfContentByte#drawTextField
         * @see PdfContentByte#drawButton
         * @see PdfContentByte#rectangle(Rectangle)
         */
        //画矩形
        pdfContentByte.saveState();
        pdfContentByte.resetRGBColorFill();
        pdfContentByte.setRGBColorFill(10,0,60);
        pdfContentByte.setTextMatrix(100,100);
        pdfContentByte.setLineWidth(20);
        pdfContentByte.setColorStroke(BaseColor.RED);
        pdfContentByte.setLineDash(1,0);
        pdfContentByte.setLineCap(PdfContentByte.LINE_CAP_BUTT);
        pdfContentByte.moveTo(100,100);
        pdfContentByte.lineTo(200,100);
        pdfContentByte.stroke();
        pdfContentByte.setColorStroke(BaseColor.BLUE);
        pdfContentByte.setLineDash(2,0);
        pdfContentByte.setLineCap(PdfContentByte.LINE_CAP_PROJECTING_SQUARE);
        pdfContentByte.moveTo(200,100);
        pdfContentByte.lineTo(200,150);
        pdfContentByte.stroke();
        pdfContentByte.setColorStroke(BaseColor.YELLOW);
        pdfContentByte.setLineDash(3,0);
        pdfContentByte.setLineCap(PdfContentByte.LINE_CAP_ROUND);
        pdfContentByte.moveTo(200,150);
        pdfContentByte.lineTo(100,150);
        pdfContentByte.stroke();
        pdfContentByte.setColorStroke(BaseColor.GRAY);
        pdfContentByte.setLineDash(2,1,0);
        pdfContentByte.moveTo(100,150);
        pdfContentByte.lineTo(100,100);
        pdfContentByte.stroke();
        pdfContentByte.restoreState();

        /**
         * @see PdfContentByte#drawTextField
         * @see PdfContentByte#drawButton
         * @see PdfContentByte#rectangle(Rectangle)
         */
        //画矩形
        pdfContentByte.saveState();
        pdfContentByte.setColorStroke(BaseColor.GREEN);
        pdfContentByte.setLineWidth(2);
        //正方形线条
        pdfContentByte.setLineCap(PdfContentByte.LINE_CAP_BUTT);
        //圆形线条
        pdfContentByte.setLineCap(PdfContentByte.LINE_CAP_PROJECTING_SQUARE);
        //矩形线条
        pdfContentByte.setLineCap(PdfContentByte.LINE_CAP_ROUND);
//        pdfContentByte.setLineDash(3,0);
        pdfContentByte.setLineDash(2,1,0);
        pdfContentByte.rectangle(130,130,200,260);
        pdfContentByte.stroke();
        pdfContentByte.restoreState();

        //画矩形
        pdfContentByte.saveState();
        pdfContentByte.setColorStroke(BaseColor.RED);
        pdfContentByte.setLineWidth(2);
        //正方形线条
        pdfContentByte.setLineCap(PdfContentByte.LINE_CAP_BUTT);
        //圆形线条
        pdfContentByte.setLineCap(PdfContentByte.LINE_CAP_PROJECTING_SQUARE);
        //矩形线条
        pdfContentByte.setLineCap(PdfContentByte.LINE_CAP_ROUND);
//        pdfContentByte.setLineDash(3,0);
        //unitson 每小段线条的长度，unitsOff 线条间隔：1、相连，2、相隔(相隔一个小圆点) 3、分离
        pdfContentByte.setLineDash(2,1,1);
//        pdfContentByte.setLineDash(2,2,0);
//        pdfContentByte.setLineDash(2,3,0);
        pdfContentByte.rectangle(135,135,200,260);
        pdfContentByte.stroke();
        pdfContentByte.restoreState();
    }

    private void drawText(PdfWriter writer, Document document) throws IOException, DocumentException {
        PdfContentByte pdfContentByte = writer.getDirectContent();
        BaseFont baseFont = BaseFont.createFont("config/font/华康少女文字简W5.ttc,1",BaseFont.IDENTITY_H,BaseFont.NOT_EMBEDDED);
        /**
         * @see PdfContentByte#drawButton
         * @see PdfAcroForm#drawCheckBoxAppearences
         */
        //写文本
        //状态保存点
        pdfContentByte.saveState();
        //重置填充颜色
        pdfContentByte.resetRGBColorFill();
        //文本开始
        pdfContentByte.beginText();
        //设置文本字体属性
        pdfContentByte.setFontAndSize(baseFont,12);
        //写文本
        pdfContentByte.showTextAligned(PdfContentByte.ALIGN_CENTER, "文本写入演示",(document.getPageSize().getRight()-document.getPageSize().getLeft())/2,document.getPageSize().getBottom()+20,0);
        pdfContentByte.showTextAligned(PdfContentByte.ALIGN_LEFT, "文本写入演示",100,100,0);
        //文本结束
        pdfContentByte.endText();
        //保存状态
        pdfContentByte.restoreState();

        /**
         * @see BarcodeInter25#placeBarcode
         */
        //写入文本
        pdfContentByte.beginText();
        pdfContentByte.setFontAndSize(baseFont,12);
        pdfContentByte.setTextMatrix(150,160);
        pdfContentByte.showText("Text");
        //文本上调下调，改变y坐标
        pdfContentByte.setTextRise(-5);
        pdfContentByte.setTextMatrix(150,160);
//        pdfContentByte.drawTextField(130,130,150,190);
        pdfContentByte.showTextKerned("TextKerned");
        pdfContentByte.endText();
    }

    private void drawImage(PdfWriter writer, Document document) throws IOException, DocumentException {
        PdfContentByte pdfContentByte = writer.getDirectContent();
        /**
         * @see com.itextpdf.text.pdf.codec.wmf.MetaDo#readAll
         * @see PdfDocument#add(Image)
         * @see com.itextpdf.awt.PdfGraphics2D#setPaint(boolean, double, double, boolean)
         */
        Image image = Image.getInstance("D://bd.jpg");
        image.setAbsolutePosition(250,160);
        image.scaleToFit(new Rectangle(0,0,100,100));
        pdfContentByte.addImage(image);
    }
}

