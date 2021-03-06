package org.rpi.plugin.oled;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.rpi.player.PlayManager;
import org.rpi.plugingateway.PluginGateWay;

public class ScrollerThread implements Runnable {

              private Logger log = Logger.getLogger(ScrollerThread.class);

              private boolean isRunning = true;
              private String text = "";
              private Font font = null;

              protected int width = 128;
              protected int height = 64;
              protected SSD1306 ssd1306 = null;

              protected int displayWidth = 0;
              protected int displayHeight = 0;

              BufferedImage imgTrack = null;
              BufferedImage imgTime = null;

              private int paddingSpace = 0;
              private boolean bScroll = false;

              private int pauseTimer = 0;

              private final int DC_BIT = 6;

              List<byte[]> titleImages = new ArrayList<byte[]>();
              Raster timeRaster = null;
              int timeHeight = 50;// At what height should the time be located.

              private String oldText = "";

              private String duration = "00:00";
              private String volume = "0";

              /***
              * 
               * @param ssd1306
              */
              public ScrollerThread(SSD1306 ssd1306) {
                             this.ssd1306 = ssd1306;
                             volume = "" + PlayManager.getInstance().getVolume();
              }

              /***
              * Set the Duration of the Track
              * 
               * @param duration
              */
              public void setDuration(String duration) {
                             this.duration = duration;

              }

              /***
              * Set the Time portion of the display
              * 
               * @param time
              */
              public void setTime(String time) {
                             String t = time;
                             if (!duration.equalsIgnoreCase("0:00")) {
                                           t += " " + duration;
                             }
                             Raster r = createRaster(t + "       " + volume, 0, 50, new Font("Arial", Font.PLAIN, 12));
                             timeRaster = r;
              }

              /***
              * Set the Volume
              * 
               * @param volume
              */
              public void setVolume(String volume) {
                             this.volume = volume;
              }

              /***
              * Set the Title of a Track
              * 
               * @param text
              * @param font
              * @param x
              * @param y
              */
              public void setTitle(String text, Font font, int x, int y) {
                             if (!text.equals(this.text)) {
                                           log.debug("SetTitle: " + text);
                                           this.text = text;
                                           this.font = font;
                                           bScroll = false;
                                           ssd1306.clear();
                                           ssd1306.display();
                                           createTitleImages(x, y);
                                           log.debug("SetTitle Ended: " + text);
                                           bScroll = true;
                             }
              }

              /***
              * Create a List of Image byte Arrays used to scroll the title text.
              * 
               * @param x
              * @param y
              */
              private void createTitleImages(int x, int y) {
                             log.debug("ScrollString: " + text);
                             // get the size of a space
                             int space = getTextWidth(font, " ");
                             // How many spaces will fill the screen
                             paddingSpace = (width / space) + 3;
                             StringBuilder sb = new StringBuilder(paddingSpace);
                             for (int i = 0; i < paddingSpace; i++) {
                                           sb.append(" ");
                             }
                             // Pad the string on both sides
                             text = sb.toString() + text + sb.toString();
                             Raster r = createRaster(text, x, y, font);
                             int rh = r.getHeight();
                             int rw = r.getWidth();

                             int stepSize = 3;

                             List<byte[]> list = new ArrayList<byte[]>();

                             if (rw > width) {
                                           int iStep = paddingSpace;
                                           // while (!text.equals(oldText)) {
                                           while (iStep < rw) {
                                                          // The raster is wider than the screen so iterate around
                                                          // it.
                                                          byte[] buffer = new byte[1024 + 1];
                                                          buffer[0] = (byte) (1 << DC_BIT);
                                                          for (int rasterY = 0; rasterY <= rh - 1; rasterY++) {
                                                                        for (int rasterX = 0; rasterX <= width; rasterX++) {
                                                                                      try {
                                                                                                     boolean isPixel = r.getSample(rasterX + iStep, rasterY, 0) > 0;
                                                                                                     setPixel(rasterX, rasterY, isPixel, buffer);
                                                                                      } catch (Exception e) {
                                                                                                     // System.out.println("X= " + rasterX +
                                                                                                     // iStep + " Y=
                                                                                                     // " + rasterY);
                                                                                      }
                                                                        }
                                                          }
                                                          list.add(buffer);
                                                          iStep = iStep + stepSize;
                                           }
                                           titleImages = list;
                                           oldText = text;
                             }
              }

              /***
              * Create a Raster for the Text, based on the Font.
              * 
               * @param text
              * @param x
              * @param y
              * @param font
              * @return
              */
              private Raster createRaster(String text, int x, int y, Font font) {
                             // log.debug("Create Raster: " + text);
                             int heightOffset = 0;
                             TextHeight h = getTextHeight(font, text, heightOffset);
                             int w = getTextWidth(font, text);

                             // log.debug("Width: " + w + " Height: " + h);
                             BufferedImage i = new BufferedImage(w, h.getAscent(), BufferedImage.TYPE_BYTE_BINARY);
                             Graphics2D g = i.createGraphics();

                             FontMetrics fm = g.getFontMetrics(font);
                             int maxDescent = fm.getMaxDescent();

                             g.setFont(font);
                             g.drawString(text, 0, h.getAscent() - maxDescent);

                             Raster r = i.getRaster();
                             return r;
              }

              /***
              * Set the Byte
              * 
               * @param x
              * @param y
              * @param on
              * @param buffer
              * @return
              */
              public boolean setPixel(int x, int y, boolean on, byte[] buffer) {
                             if (x < 0 || x >= width || y < 0 || y >= height) {
                                           return false;
                             }
                             int myY = ((y / 8) * width) + 1;
                             if (on) {
                                           buffer[x + myY] |= (1 << (y & 7));
                             } else {
                                           buffer[x + myY] &= ~(1 << (y & 7));
                             }
                             return true;
              }

              /***
              * Get the Expected Width of the Text
              * 
               * @param font
              * @param text
              * @return
              */
              public int getTextWidth(java.awt.Font font, String text) {
                             FontMetrics metrics = new FontMetrics(font) {
                             };
                             Rectangle2D bounds = metrics.getStringBounds(text, null);
                             return (int) bounds.getWidth();
              }

              /***
              * Get the Expected Height of the Text
              * 
               * @param font
              * @param text
              * @param heightOffset
              * @return
              */
              public TextHeight getTextHeight(java.awt.Font font, String text, int heightOffset) {
                             FontMetrics metrics = new FontMetrics(font) {
                             };
                             int ascent = metrics.getMaxAscent();
                             int descent = metrics.getMaxDescent();
                             TextHeight th = new TextHeight(ascent, descent);
                             return th;
              }

              @Override
              public void run() {
                             while (isRunning) {

                                           boolean isMe = true;
                                           while (isMe) {
                                                          if (pauseTimer > 0) {
                                                                        try {
                                                                                      log.debug("Scroller Paused: " + pauseTimer);
                                                                                      Thread.sleep(1000);
                                                                        } catch (InterruptedException e) {

                                                                        }
                                                                        pauseTimer--;

                                                          } else {
                                                                        try {
                                                                                      // int lastHashCode = 0;
                                                                                      // Iterate each Frame.
                                                                                      for (byte[] b : titleImages) {
                                                                                                     if (!bScroll && pauseTimer > 0) {
                                                                                                                   break;
                                                                                                     }
                                                                                                     // Sort out the time part..
                                                                                                     if (timeRaster != null) {
                                                                                                                   int rh = timeRaster.getHeight();
                                                                                                                   int rw = timeRaster.getWidth();
                                                                                                                   for (int rasterY = 0; rasterY <= rh - 1; rasterY++) {
                                                                                                                                  if (!bScroll && pauseTimer > 0) {
                                                                                                                                                break;
                                                                                                                                  }
                                                                                                                                  for (int rasterX = 0; rasterX <= rw - 1; rasterX++) {
                                                                                                                                                if (!bScroll && pauseTimer > 0) {
                                                                                                                                                               break;
                                                                                                                                                }
                                                                                                                                                try {
                                                                                                                                                               boolean isPixel = timeRaster.getSample(rasterX + 0, rasterY, 0) > 0;
                                                                                                                                                               setPixel(rasterX, rasterY + 50, isPixel, b);
                                                                                                                                                } catch (Exception e) {
                                                                                                                                                               e.printStackTrace();
                                                                                                                                                }
                                                                                                                                  }
                                                                                                                   }
                                                                                                     }
                                                                                                     // int hashCode = Arrays.hashCode(b);
                                                                                                     // if (lastHashCode != hashCode) {
                                                                                                     if (bScroll && pauseTimer == 0) {
                                                                                                                   ssd1306.myData(b);
                                                                                                     }
                                                                                                     // }
                                                                                                     // lastHashCode = hashCode;
                                                                                      }
                                                                        } catch (Exception e) {
                                                                                      log.error(e);
                                                                        }
                                                          }
                                           }
                             }
                             log.info("Scroller Stopped");
              }

              /***
              * 
               * @param pauseTimer
              */
              public void setPauseTimer(int pauseTimer) {
                             this.pauseTimer = pauseTimer;
              }

              /***
              * 
               * @param bScroll
              */
              public void setScroll(boolean bScroll) {
                             this.bScroll = bScroll;
              }

              /***
              * Stop the Scroller
              */
              public void stop() {
                             log.info("Stopping the Scroller");
                             isRunning = false;
              }

              /***
              * Get the Value of the Pause Timer
              * 
               * @return
              */
              public int getPauseTimer() {
                             return pauseTimer;
              }

}
