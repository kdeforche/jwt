/*
 * Copyright (C) 2009 Emweb bvba, Leuven, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt.chart;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.lang.ref.*;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.http.*;
import javax.servlet.*;
import eu.webtoolkit.jwt.*;
import eu.webtoolkit.jwt.chart.*;
import eu.webtoolkit.jwt.utils.*;
import eu.webtoolkit.jwt.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for iterating over series data in a chart.
 * <p>
 * 
 * This class is specialized for rendering series data.
 * <p>
 */
public class SeriesIterator {
	private static Logger logger = LoggerFactory
			.getLogger(SeriesIterator.class);

	/**
	 * Start handling a new segment.
	 * <p>
	 * Because of a &apos;break&apos; specified in an axis, axes may be divided
	 * in one or two segments (in fact only the API limits this now to two). The
	 * iterator will iterate all segments seperately, but each time with a
	 * different clipping region specified in the painter, corresponding to that
	 * segment.
	 * <p>
	 * The <i>currentSegmentArea</i> specifies the clipping area.
	 */
	public void startSegment(int currentXSegment, int currentYSegment,
			final WRectF currentSegmentArea) {
		this.currentXSegment_ = currentXSegment;
		this.currentYSegment_ = currentYSegment;
	}

	/**
	 * End handling a particular segment.
	 * <p>
	 * 
	 * @see SeriesIterator#startSegment(int currentXSegment, int
	 *      currentYSegment, WRectF currentSegmentArea)
	 */
	public void endSegment() {
	}

	/**
	 * Start iterating a particular series.
	 * <p>
	 * Returns whether the series values should be iterated. The
	 * <i>groupWidth</i> is the width (in pixels) of a single bar group. The
	 * chart contains <i>numBarGroups</i>, and the current series is in the
	 * <i>currentBarGroup</i>&apos;th group.
	 */
	public boolean startSeries(final WDataSeries series, double groupWidth,
			int numBarGroups, int currentBarGroup) {
		return true;
	}

	/**
	 * End iterating a particular series.
	 */
	public void endSeries() {
	}

	/**
	 * Process a value.
	 * <p>
	 * Processes a value with model coordinates (<i>x</i>, <i>y</i>). The y
	 * value may differ from the model&apos;s y value, because of stacked
	 * series. The y value here corresponds to the location on the chart, after
	 * stacking.
	 * <p>
	 * The <i>stackY</i> argument is the y value from the previous series (also
	 * after stacking). It will be 0, unless this series is stacked.
	 */
	public void newValue(final WDataSeries series, double x, double y,
			double stackY, int xRow, int xColumn, int yRow, int yColumn) {
	}

	/**
	 * Returns the current X segment.
	 */
	public int getCurrentXSegment() {
		return this.currentXSegment_;
	}

	/**
	 * Returns the current Y segment.
	 */
	public int getCurrentYSegment() {
		return this.currentYSegment_;
	}

	public static void setPenColor(final WPen pen, final WDataSeries series,
			int xRow, int xColumn, int yRow, int yColumn, int colorRole) {
		WColor color = null;
		if (yRow >= 0 && yColumn >= 0) {
			if (colorRole == ItemDataRole.MarkerPenColorRole) {
				color = series.getModel().getMarkerPenColor(yRow, yColumn);
			} else {
				if (colorRole == ItemDataRole.MarkerBrushColorRole) {
					color = series.getModel()
							.getMarkerBrushColor(yRow, yColumn);
				}
			}
		}
		if (!(color != null) && xRow >= 0 && xColumn >= 0) {
			if (colorRole == ItemDataRole.MarkerPenColorRole) {
				color = series.getModel().getMarkerPenColor(xRow, xColumn);
			} else {
				if (colorRole == ItemDataRole.MarkerBrushColorRole) {
					color = series.getModel()
							.getMarkerBrushColor(xRow, xColumn);
				}
			}
		}
		if (color != null) {
			pen.setColor(color);
		}
	}

	public static void setBrushColor(final WBrush brush,
			final WDataSeries series, int xRow, int xColumn, int yRow,
			int yColumn, int colorRole) {
		WColor color = null;
		if (yRow >= 0 && yColumn >= 0) {
			if (colorRole == ItemDataRole.MarkerBrushColorRole) {
				color = series.getModel().getMarkerBrushColor(yRow, yColumn);
			} else {
				if (colorRole == ItemDataRole.BarBrushColorRole) {
					color = series.getModel().getBarBrushColor(yRow, yColumn);
				}
			}
		}
		if (!(color != null) && xRow >= 0 && xColumn >= 0) {
			if (colorRole == ItemDataRole.MarkerBrushColorRole) {
				color = series.getModel().getMarkerBrushColor(xRow, xColumn);
			} else {
				if (colorRole == ItemDataRole.BarBrushColorRole) {
					color = series.getModel().getBarBrushColor(xRow, xColumn);
				}
			}
		}
		if (color != null) {
			brush.setColor(color);
		}
	}

	private int currentXSegment_;
	private int currentYSegment_;

	static WJavaScriptPreamble wtjs2() {
		return new WJavaScriptPreamble(
				JavaScriptScope.WtClassScope,
				JavaScriptObjectType.JavaScriptConstructor,
				"ChartCommon",
				"function(B){function F(a,b,c,d){function e(m){return c?b[m]:b[n-1-m]}function i(m){for(;e(m)[2]===y||e(m)[2]===C;)m--;return m}var j=k;if(d)j=l;var n=b.length;d=Math.floor(n/2);d=i(d);var t=0,r=n,f=false;if(e(0)[j]>a)return c?-1:n;if(e(n-1)[j]<a)return c?n:-1;for(;!f;){var g=d+1;if(g<n&&(e(g)[2]===y||e(g)[2]===C))g+=2;if(e(d)[j]>a){r=d;d=Math.floor((r+t)/2);d=i(d)}else if(e(d)[j]===a)f=true;else if(g<n&&e(g)[j]>a)f=true;else if(g<n&&e(g)[j]=== a){d=g;f=true}else{t=d;d=Math.floor((r+t)/2);d=i(d)}}return c?d:n-1-d}function H(a,b){return b[0][a]<b[b.length-1][a]}var y=2,C=3,k=0,l=1,G=this;B=B.WT.gfxUtils;var z=B.rect_top,A=B.rect_bottom,v=B.rect_left,D=B.rect_right,I=B.transform_mult;this.findClosestPoint=function(a,b,c){var d=k;if(c)d=l;var e=H(d,b);c=F(a,b,e,c);if(c<0)c=0;if(c>=b.length)return[b[b.length-1][k],b[b.length-1][l]];if(c>=b.length)c=b.length-2;if(b[c][d]===a)return[b[c][k],b[c][l]];var i=e?c+1:c-1;if(e&&b[i][2]==y)i+=2;if(!e&& i<0)return[b[c][k],b[c][l]];if(!e&&i>0&&b[i][2]==C)i-=2;e=Math.abs(a-b[c][d]);a=Math.abs(b[i][d]-a);return e<a?[b[c][k],b[c][l]]:[b[i][k],b[i][l]]};this.minMaxY=function(a,b){b=b?k:l;for(var c=a[0][b],d=a[0][b],e=1;e<a.length;++e)if(a[e][2]!==y&&a[e][2]!==C&&a[e][2]!==5){if(a[e][b]>d)d=a[e][b];if(a[e][b]<c)c=a[e][b]}return[c,d]};this.projection=function(a,b){var c=Math.cos(a);a=Math.sin(a);var d=c*a,e=-b[0]*c-b[1]*a;return[c*c,d,d,a*a,c*e+b[0],a*e+b[1]]};this.distanceSquared=function(a,b){a=[b[k]- a[k],b[l]-a[l]];return a[k]*a[k]+a[l]*a[l]};this.distanceLessThanRadius=function(a,b,c){return c*c>=G.distanceSquared(a,b)};this.toZoomLevel=function(a){return Math.floor(Math.log(a)/Math.LN2+0.5)+1};this.isPointInRect=function(a,b){var c;if(a.x!==undefined){c=a.x;a=a.y}else{c=a[0];a=a[1]}return c>=v(b)&&c<=D(b)&&a>=z(b)&&a<=A(b)};this.toDisplayCoord=function(a,b,c,d,e){if(c){a=[(a[k]-e[0])/e[2],(a[l]-e[1])/e[3]];d=[d[0]+a[l]*d[2],d[1]+a[k]*d[3]]}else{a=[(a[k]-e[0])/e[2],1-(a[l]-e[1])/e[3]];d=[d[0]+ a[k]*d[2],d[1]+a[l]*d[3]]}return I(b,d)};this.findYRange=function(a,b,c,d,e,i,j,n,t){if(a.length!==0){var r=G.toDisplayCoord([c,0],[1,0,0,1,0,0],e,i,j),f=G.toDisplayCoord([d,0],[1,0,0,1,0,0],e,i,j),g=e?l:k,m=e?k:l,o=H(g,a),h=F(r[g],a,o,e),p=F(f[g],a,o,e),q,s,w=Infinity,x=-Infinity,E=h===p&&h===a.length||h===-1&&p===-1;if(!E){if(o)if(h<0)h=0;else{h++;if(a[h]&&a[h][2]===y)h+=2}else if(h>=a.length-1)h=a.length-2;if(!o&&p<0)p=0;for(q=Math.min(h,p);q<=Math.max(h,p)&&q<a.length;++q)if(a[q][2]!==y&&a[q][2]!== C){if(a[q][m]<w)w=a[q][m];if(a[q][m]>x)x=a[q][m]}if(o&&h>0||!o&&h<a.length-1){if(o){s=h-1;if(a[s]&&a[s][2]===C)s-=2}else{s=h+1;if(a[s]&&a[s][2]===y)s+=2}q=(r[g]-a[s][g])/(a[h][g]-a[s][g]);h=a[s][m]+q*(a[h][m]-a[s][m]);if(h<w)w=h;if(h>x)x=h}if(o&&p<a.length-1||!o&&p>0){if(o){o=p+1;if(a[o][2]===y)o+=2}else{o=p-1;if(a[o][2]===C)o-=2}q=(f[g]-a[p][g])/(a[o][g]-a[p][g]);h=a[p][m]+q*(a[o][m]-a[p][m]);if(h<w)w=h;if(h>x)x=h}}var u;a=j[2]/(d-c);c=e?2:3;if(!E){u=i[c]/(x-w);u=i[c]/(i[c]/u+20);if(u>t.y[b])u=t.y[b]; if(u<n.y[b])u=n.y[b]}b=e?[r[l]-z(i),!E?(w+x)/2-i[2]/u/2-v(i):0]:[r[k]-v(i),!E?-((w+x)/2+i[3]/u/2-A(i)):0];return{xZoom:a,yZoom:u,panPoint:b}}};this.matchesXAxis=function(a,b,c,d,e){if(e){if(b<z(c)||b>A(c))return false;if((d.side===\"min\"||d.side===\"both\")&&a>=v(c)-d.width&&a<=v(c))return true;if((d.side===\"max\"||d.side===\"both\")&&a<=D(c)+d.width&&a>=D(c))return true}else{if(a<v(c)||a>D(c))return false;if((d.side===\"min\"||d.side===\"both\")&&b<=A(c)+d.width&&b>=A(c))return true;if((d.side===\"max\"||d.side=== \"both\")&&b>=z(c)-d.width&&b<=z(c))return true}return false};this.matchYAxis=function(a,b,c,d,e){function i(){return d.length}function j(g){return d[g].side}function n(g){return d[g].width}function t(g){return d[g].minOffset}function r(g){return d[g].maxOffset}if(e){if(a<v(c)||a>D(c))return-1}else if(b<z(c)||b>A(c))return-1;for(var f=0;f<i();++f)if(e)if((j(f)===\"min\"||j(f)===\"both\")&&b>=z(c)-t(f)-n(f)&&b<=z(c)-t(f))return f;else{if((j(f)===\"max\"||j(f)===\"both\")&&b>=A(c)+r(f)&&b<=A(c)+r(f)+n(f))return f}else if((j(f)=== \"min\"||j(f)===\"both\")&&a>=v(c)-t(f)-n(f)&&a<=v(c)-t(f))return f;else if((j(f)===\"max\"||j(f)===\"both\")&&a>=D(c)+r(f)&&a<=D(c)+r(f)+n(f))return f;return-1}}");
	}

	static WJavaScriptPreamble wtjs1() {
		return new WJavaScriptPreamble(
				JavaScriptScope.WtClassScope,
				JavaScriptObjectType.JavaScriptConstructor,
				"WCartesianChart",
				"function(wa,J,z,l){function M(a){return a===undefined}function o(a){return l.modelAreas[a]}function U(){return l.followCurve}function xa(){return l.crosshair||U()!==-1}function A(){return l.isHorizontal}function j(){return l.xTransform}function g(a){return l.yTransforms[a]}function h(){return l.area}function n(){return l.insideArea}function ca(a){return M(a)?l.series:l.series[a]}function da(a){return ca(a).transform}function kb(a){return A()? w([0,1,1,0,0,0],w(da(a),[0,1,1,0,0,0])):da(a)}function Pa(a){return ca(a).curve}function P(a){return ca(a).axis}function lb(){return l.seriesSelection}function mb(){return l.sliders}function nb(){return l.hasToolTips}function ob(){return l.coordinateOverlayPadding}function Ga(){return l.curveManipulation}function Qa(){return l.minZoom.x}function pb(a){return l.minZoom.y[a]}function ia(){return l.maxZoom.x}function V(a){return l.maxZoom.y[a]}function N(){return l.pens}function qb(){return l.penAlpha} function ea(){return l.selectedCurve}function ya(a){a.preventDefault&&a.preventDefault()}function fa(a,b){J.addEventListener(a,b)}function W(a,b){J.removeEventListener(a,b)}function C(a){return a.length}function K(){return C(l.yTransforms)}function Bb(){if(l.notifyTransform.x)return true;for(var a=0;a<K();++a)if(l.notifyTransform.y[a])return true;return false}function Q(){return l.crosshairAxis}function bb(a){return a.pointerType===2||a.pointerType===3||a.pointerType===\"pen\"||a.pointerType===\"touch\"} function Ra(){if(p){if(p.tooltipTimeout){clearTimeout(p.tooltipTimeout);p.tooltipTimeout=null}if(!p.overTooltip)if(p.tooltipOuterDiv){document.body.removeChild(p.tooltipOuterDiv);p.tooltipEl=null;p.tooltipOuterDiv=null}}}function Ha(){if(Bb()){if(Sa){window.clearTimeout(Sa);Sa=null}Sa=setTimeout(function(){if(l.notifyTransform.x&&!rb(cb,j())){wa.emit(z.widget,\"xTransformChanged\");ja(cb,j())}for(var a=0;a<K();++a)if(l.notifyTransform.y[a]&&!rb(Ta[a],g(a))){wa.emit(z.widget,\"yTransformChanged\"+a);ja(Ta[a], g(a))}},Cb)}}function ka(a){var b,c;if(A()){b=q(h());c=x(h());return w([0,1,1,0,b,c],w(j(),w(g(a),[0,1,1,0,-c,-b])))}else{b=q(h());c=y(h());return w([1,0,0,-1,b,c],w(j(),w(g(a),[1,0,0,-1,-b,c])))}}function F(a){return w(ka(a),n())}function la(a,b,c){if(M(c))c=false;a=c?a:w(Ia(ka(b)),a);a=A()?[(a[u]-h()[1])/h()[3],(a[v]-h()[0])/h()[2]]:[(a[v]-h()[0])/h()[2],1-(a[u]-h()[1])/h()[3]];return[o(b)[0]+a[v]*o(b)[2],o(b)[1]+a[u]*o(b)[3]]}function Ua(a,b,c){if(M(c))c=false;return X.toDisplayCoord(a,c?[1,0, 0,1,0,0]:ka(b),A(),h(),o(b))}function Ja(){var a,b;if(A()){a=(la([0,x(h())],0)[0]-o(0)[0])/o(0)[2];b=(la([0,y(h())],0)[0]-o(0)[0])/o(0)[2]}else{a=(la([q(h()),0],0)[0]-o(0)[0])/o(0)[2];b=(la([s(h()),0],0)[0]-o(0)[0])/o(0)[2]}var c;for(c=0;c<C(mb());++c){var d=$(\"#\"+mb()[c]);if(d)(d=d.data(\"sobj\"))&&d.changeRange(a,b)}}function Y(){Ra();if(nb()&&p.tooltipPosition)p.tooltipTimeout=setTimeout(function(){sb()},tb);ma&&ub(function(){z.repaint();xa()&&db()})}function db(){if(ma){var a=I.getContext(\"2d\"); a.clearRect(0,0,I.width,I.height);a.save();a.beginPath();a.moveTo(q(h()),x(h()));a.lineTo(s(h()),x(h()));a.lineTo(s(h()),y(h()));a.lineTo(q(h()),y(h()));a.closePath();a.clip();var b=w(Ia(ka(Q())),B),c=B[v],d=B[u];if(U()!==-1){b=Db(A()?b[u]:b[v],Pa(U()),A());d=w(ka(P(U())),w(kb(U()),b));c=d[v];d=d[u];B[v]=c;B[u]=d}b=A()?[(b[u]-h()[1])/h()[3],(b[v]-h()[0])/h()[2]]:[(b[v]-h()[0])/h()[2],1-(b[u]-h()[1])/h()[3]];b=U()!==-1?[o(P(U()))[0]+b[v]*o(P(U()))[2],o(P(U()))[1]+b[u]*o(P(U()))[3]]:[o(Q())[0]+b[v]* o(Q())[2],o(Q())[1]+b[u]*o(Q())[3]];a.fillStyle=a.strokeStyle=l.crosshairColor;a.font=\"16px sans-serif\";a.textAlign=\"right\";a.textBaseline=\"top\";var e=b[0].toFixed(2);b=b[1].toFixed(2);if(e===\"-0.00\")e=\"0.00\";if(b===\"-0.00\")b=\"0.00\";a.fillText(\"(\"+e+\",\"+b+\")\",s(h())-ob()[0],x(h())+ob()[1]);a.setLineDash&&a.setLineDash([1,2]);a.beginPath();a.moveTo(Math.floor(c)+0.5,Math.floor(x(h()))+0.5);a.lineTo(Math.floor(c)+0.5,Math.floor(y(h()))+0.5);a.moveTo(Math.floor(q(h()))+0.5,Math.floor(d)+0.5);a.lineTo(Math.floor(s(h()))+ 0.5,Math.floor(d)+0.5);a.stroke();a.restore()}}function Eb(a){return x(a)<=x(n())+Va&&y(a)>=y(n())-Va&&q(a)<=q(n())+Va&&s(a)>=s(n())-Va}function ga(a){for(var b=0;b<K();++b){var c=F(b);if(A())if(a===za)a=Aa;else if(a===Aa)a=za;if(M(a)||a===za)if(j()[0]<1){j()[0]=1;c=F(b)}if(M(a)||a===Aa)if(g(b)[3]<1){g(b)[3]=1;c=F(b)}if(M(a)||a===za){if(q(c)>q(n())){c=q(n())-q(c);if(A())g(b)[5]=g(b)[5]+c;else j()[4]=j()[4]+c;c=F(b)}if(s(c)<s(n())){c=s(n())-s(c);if(A())g(b)[5]=g(b)[5]+c;else j()[4]=j()[4]+c;c=F(b)}}if(M(a)|| a===Aa){if(x(c)>x(n())){c=x(n())-x(c);if(A())j()[4]=j()[4]+c;else g(b)[5]=g(b)[5]-c;c=F(b)}if(y(c)<y(n())){c=y(n())-y(c);if(A())j()[4]=j()[4]+c;else g(b)[5]=g(b)[5]-c;F(b)}}}Ha()}function sb(){wa.emit(z.widget,\"loadTooltip\",p.tooltipPosition[v],p.tooltipPosition[u])}function Fb(){if(xa()&&(M(I)||z.canvas.width!==I.width||z.canvas.height!==I.height)){if(I){I.parentNode.removeChild(I);jQuery.removeData(J,\"oobj\");I=undefined}var a=document.createElement(\"canvas\");a.setAttribute(\"width\",z.canvas.width); a.setAttribute(\"height\",z.canvas.height);a.style.position=\"absolute\";a.style.display=\"block\";a.style.left=\"0\";a.style.top=\"0\";if(window.MSPointerEvent||window.PointerEvent){a.style.msTouchAction=\"none\";a.style.touchAction=\"none\"}z.canvas.parentNode.appendChild(a);I=a;jQuery.data(J,\"oobj\",I)}else if(!M(I)&&!xa()){I.parentNode.removeChild(I);jQuery.removeData(J,\"oobj\");I=undefined}B=[(q(h())+s(h()))/2,(x(h())+y(h()))/2]}function vb(){return I?I:z.canvas}function eb(a,b){if(Ba){var c=Date.now();if(M(b))b= c-na;var d={x:0,y:0},e;if(G)e=F(0);else if(t===-1){e=F(0);for(var f=1;f<K();++f)e=Wa(e,F(f))}else e=F(t);f=Gb;if(b>2*Ka){ma=false;var i=Math.floor(b/Ka-1),m;for(m=0;m<i;++m){eb(a,Ka);if(!Ba){ma=true;Y();return}}b-=i*Ka;ma=true}if(k.x===Infinity||k.x===-Infinity)k.x=k.x>0?oa:-oa;if(isFinite(k.x)){k.x/=1+wb*b;e[0]+=k.x*b;if(q(e)>q(n())){k.x+=-f*(q(e)-q(n()))*b;k.x*=0.7}else if(s(e)<s(n())){k.x+=-f*(s(e)-s(n()))*b;k.x*=0.7}if(Math.abs(k.x)<fb)if(q(e)>q(n()))k.x=fb;else if(s(e)<s(n()))k.x=-fb;if(Math.abs(k.x)> oa)k.x=(k.x>0?1:-1)*oa;d.x=k.x*b}if(k.y===Infinity||k.y===-Infinity)k.y=k.y>0?oa:-oa;if(isFinite(k.y)){k.y/=1+wb*b;e[1]+=k.y*b;if(x(e)>x(n())){k.y+=-f*(x(e)-x(n()))*b;k.y*=0.7}else if(y(e)<y(n())){k.y+=-f*(y(e)-y(n()))*b;k.y*=0.7}if(Math.abs(k.y)<0.001)if(x(e)>x(n()))k.y=0.001;else if(y(e)<y(n()))k.y=-0.001;if(Math.abs(k.y)>oa)k.y=(k.y>0?1:-1)*oa;d.y=k.y*b}if(G)e=F(0);else if(t===-1){e=F(0);for(f=1;f<K();++f)e=Wa(e,F(f))}else e=F(t);Z(d,Ca,t,G);if(G)a=F(0);else if(t===-1){a=F(0);for(f=1;f<K();++f)a= Wa(a,F(f))}else a=F(t);if(q(e)>q(n())&&q(a)<=q(n())){k.x=0;Z({x:-d.x,y:0},Ca,t,G);ga(za)}if(s(e)<s(n())&&s(a)>=s(n())){k.x=0;Z({x:-d.x,y:0},Ca,t,G);ga(za)}if(x(e)>x(n())&&x(a)<=x(n())){k.y=0;Z({x:0,y:-d.y},Ca,t,G);ga(Aa)}if(y(e)<y(n())&&y(a)>=y(n())){k.y=0;Z({x:0,y:-d.y},Ca,t,G);ga(Aa)}if(Math.abs(k.x)<xb&&Math.abs(k.y)<xb&&Eb(a)){ga();Ba=false;D=null;k.x=0;k.y=0;na=null;r=[]}else{na=c;ma&&Xa(eb)}}}function Ya(){var a,b,c=yb(j()[0])-1;if(c>=C(N().x))c=C(N().x)-1;for(a=0;a<C(N().x);++a)if(c===a)for(b= 0;b<C(N().x[a]);++b)N().x[a][b].color[3]=qb().x[b];else for(b=0;b<C(N().x[a]);++b)N().x[a][b].color[3]=0;for(c=0;c<C(N().y);++c){var d=yb(g(c)[3])-1;if(d>=C(N().y[c]))d=C(N().y[c])-1;for(a=0;a<C(N().y[c]);++a)if(d===a)for(b=0;b<C(N().y[c][a]);++b)N().y[c][a][b].color[3]=qb().y[c][b];else for(b=0;b<C(N().y[c][a]);++b)N().y[c][a][b].color[3]=0}}function Z(a,b,c,d){if(M(b))b=0;if(M(c))c=-1;if(M(d))d=false;var e=la(B,Q());if(A())a={x:a.y,y:-a.x};if(b&Ca){if(d)j()[4]=j()[4]+a.x;else if(c===-1){j()[4]= j()[4]+a.x;for(b=0;b<K();++b)g(b)[5]=g(b)[5]-a.y}else g(c)[5]=g(c)[5]-a.y;Ha()}else if(b&zb){var f;if(d)f=F(0);else if(c===-1){f=F(0);for(b=1;b<K();++b)f=Wa(f,F(b))}else f=F(c);if(q(f)>q(n())){if(a.x>0)a.x/=1+(q(f)-q(n()))*Za}else if(s(f)<s(n()))if(a.x<0)a.x/=1+(s(n())-s(f))*Za;if(x(f)>x(n())){if(a.y>0)a.y/=1+(x(f)-x(n()))*Za}else if(y(f)<y(n()))if(a.y<0)a.y/=1+(y(n())-y(f))*Za;if(d)j()[4]=j()[4]+a.x;else if(c===-1){j()[4]=j()[4]+a.x;for(b=0;b<K();++b)g(b)[5]=g(b)[5]-a.y}else g(c)[5]=g(c)[5]-a.y; if(c===-1)B[v]+=a.x;d||(B[u]+=a.y);Ha()}else{if(d)j()[4]=j()[4]+a.x;else if(c===-1){j()[4]=j()[4]+a.x;for(b=0;b<K();++b)g(b)[5]=g(b)[5]-a.y}else g(c)[5]=g(c)[5]-a.y;if(c===-1)B[v]+=a.x;d||(B[u]+=a.y);ga()}a=Ua(e,Q());B[v]=a[v];B[u]=a[u];Y();Ja()}function La(a,b,c,d,e){if(M(d))d=-1;if(M(e))e=false;var f=la(B,Q());a=A()?[a.y-x(h()),a.x-q(h())]:w(Ia([1,0,0,-1,q(h()),y(h())]),[a.x,a.y]);var i=a[0];a=a[1];var m=Math.pow(1.2,A()?c:b);b=Math.pow(1.2,A()?b:c);if(j()[0]*m>ia())m=ia()/j()[0];if(j()[0]*m<Qa())m= Qa()/j()[0];if(e){if(m<1||j()[0]!==ia())pa(j(),w([m,0,0,1,i-m*i,0],j()))}else if(d===-1){if(m<1||j()[0]!==ia())pa(j(),w([m,0,0,1,i-m*i,0],j()));for(d=0;d<K();++d){e=b;if(g(d)[3]*b>V(d))e=V(d)/g(d)[3];if(e<1||g(d)[3]!==V(d))pa(g(d),w([1,0,0,e,0,a-e*a],g(d)))}}else{if(g(d)[3]*b>V(d))b=V(d)/g(d)[3];if(b<1||g(d)[3]!=V(d))pa(g(d),w([1,0,0,b,0,a-b*a],g(d)))}ga();f=Ua(f,Q());B[v]=f[v];B[u]=f[u];Ya();Y();Ja()}jQuery.data(J,\"cobj\",this);var qa=this,E=wa.WT;qa.config=l;var H=E.gfxUtils,w=H.transform_mult,Ia= H.transform_inverted,ja=H.transform_assign,rb=H.transform_equal,Hb=H.transform_apply,x=H.rect_top,y=H.rect_bottom,q=H.rect_left,s=H.rect_right,Wa=H.rect_intersection,X=E.chartCommon,Ib=X.minMaxY,Db=X.findClosestPoint,Jb=X.projection,Ab=X.distanceLessThanRadius,yb=X.toZoomLevel,Ma=X.isPointInRect,Kb=X.findYRange,Na=function(a,b){return X.matchesXAxis(a,b,h(),l.xAxis,A())},Oa=function(a,b){return X.matchYAxis(a,b,h(),l.yAxes,A())},Ka=17,Xa=function(){return window.requestAnimationFrame||window.webkitRequestAnimationFrame|| window.mozRequestAnimationFrame||function(a){window.setTimeout(a,Ka)}}(),gb=false,ub=function(a){if(!gb){gb=true;Xa(function(){a();gb=false})}};if(window.MSPointerEvent||window.PointerEvent){J.style.touchAction=\"none\";z.canvas.style.msTouchAction=\"none\";z.canvas.style.touchAction=\"none\"}var Ca=1,zb=2,za=1,Aa=2,v=0,u=1,Cb=250,tb=500,wb=0.003,Gb=2.0E-4,Za=0.07,Va=3,fb=0.001,oa=1.5,xb=0.02,ta=jQuery.data(J,\"eobj2\");if(!ta){ta={};ta.contextmenuListener=function(a){ya(a);W(\"contextmenu\",ta.contextmenuListener)}}jQuery.data(J, \"eobj2\",ta);var aa={},ua=false;if(window.MSPointerEvent||window.PointerEvent)(function(){function a(){ua=C(e)>0}function b(i){if(bb(i)){ya(i);e.push(i);a();aa.start(J,{touches:e.slice(0)})}}function c(i){if(ua)if(bb(i)){ya(i);var m;for(m=0;m<C(e);++m)if(e[m].pointerId===i.pointerId){e.splice(m,1);break}a();aa.end(J,{touches:e.slice(0),changedTouches:[]})}}function d(i){if(bb(i)){ya(i);var m;for(m=0;m<C(e);++m)if(e[m].pointerId===i.pointerId){e[m]=i;break}a();aa.moved(J,{touches:e.slice(0)})}}var e= [],f=jQuery.data(J,\"eobj\");if(f)if(window.PointerEvent){W(\"pointerdown\",f.pointerDown);W(\"pointerup\",f.pointerUp);W(\"pointerout\",f.pointerUp);W(\"pointermove\",f.pointerMove)}else{W(\"MSPointerDown\",f.pointerDown);W(\"MSPointerUp\",f.pointerUp);W(\"MSPointerOut\",f.pointerUp);W(\"MSPointerMove\",f.pointerMove)}jQuery.data(J,\"eobj\",{pointerDown:b,pointerUp:c,pointerMove:d});if(window.PointerEvent){fa(\"pointerdown\",b);fa(\"pointerup\",c);fa(\"pointerout\",c);fa(\"pointermove\",d)}else{fa(\"MSPointerDown\",b);fa(\"MSPointerUp\", c);fa(\"MSPointerOut\",c);fa(\"MSPointerMove\",d)}})();var I=jQuery.data(J,\"oobj\"),B=null,ma=true,D=null,G=false,t=-1,r=[],ha=false,ra=false,T=null,hb=null,ib=null,k={x:0,y:0},ba=null,na=null,p=jQuery.data(J,\"tobj\");if(!p){p={overTooltip:false};jQuery.data(J,\"tobj\",p)}var Da=null,Ba=false,Sa=null,cb=[0,0,0,0,0,0];ja(cb,j());var Ta=[];for(H=0;H<K();++H){Ta.push([0,0,0,0,0,0]);ja(Ta[H],g(H))}var pa=function(a,b){ja(a,b);Ha()};z.combinedTransform=ka;this.updateTooltip=function(a){Ra();if(a)if(p.tooltipPosition){p.toolTipEl= document.createElement(\"div\");p.toolTipEl.className=l.ToolTipInnerStyle;p.toolTipEl.innerHTML=a;p.tooltipOuterDiv=document.createElement(\"div\");p.tooltipOuterDiv.className=l.ToolTipOuterStyle;document.body.appendChild(p.tooltipOuterDiv);p.tooltipOuterDiv.appendChild(p.toolTipEl);var b=E.widgetPageCoordinates(z.canvas);a=p.tooltipPosition[v]+b.x;b=p.tooltipPosition[u]+b.y;E.fitToWindow(p.tooltipOuterDiv,a+10,b+10,a-10,b-10);$(p.toolTipEl).mouseenter(function(){p.overTooltip=true});$(p.toolTipEl).mouseleave(function(){p.overTooltip= false})}};this.mouseMove=function(a,b){setTimeout(function(){setTimeout(Ra,200);if(!ua){var c=E.widgetCoordinates(z.canvas,b);if(Ma(c,h())){if(!p.tooltipEl&&nb()){p.tooltipPosition=[c.x,c.y];p.tooltipTimeout=setTimeout(function(){sb()},tb)}if(D===null&&xa()&&ma){B=[c.x,c.y];ub(db)}}}},0)};this.mouseOut=function(){setTimeout(Ra,200)};this.mouseDown=function(a,b){if(!ua){a=E.widgetCoordinates(z.canvas,b);b=Oa(a.x,a.y);var c=Ma(a,h()),d=Na(a.x,a.y);if(!(b===-1&&!d&&!c)){D=a;G=d;t=b}}};this.mouseUp=function(){if(!ua){D= null;G=false;t=-1}};this.mouseDrag=function(a,b){if(!ua)if(D!==null){a=E.widgetCoordinates(z.canvas,b);if(E.buttons===1)if(t===-1&&!G&&Ga()&&ca(ea())){b=ea();var c;c=A()?a.x-D.x:a.y-D.y;ja(da(b),w([1,0,0,1,0,c/g(P(seriesNb))[3]],da(b)));Y()}else l.pan&&Z({x:a.x-D.x,y:a.y-D.y},0,t,G);D=a}};this.clicked=function(a,b){if(!ua)if(D===null)if(lb()){a=E.widgetCoordinates(z.canvas,b);wa.emit(z.widget,\"seriesSelected\",a.x,a.y)}};this.mouseWheel=function(a,b){var c=(b.metaKey<<3)+(b.altKey<<2)+(b.ctrlKey<< 1)+b.shiftKey;a=l.wheelActions[c];if(!M(a)){var d=E.widgetCoordinates(z.canvas,b),e=Na(d.x,d.y),f=Oa(d.x,d.y),i=Ma(d,h());if(!(!e&&f===-1&&!i)){var m=E.normalizeWheel(b);if(i&&c===0&&Ga()){c=ea();i=-m.spinY;if(ca(c)){a=kb(c);a=Hb(a,Pa(c));a=Ib(a,A());a=(a[0]+a[1])/2;E.cancelEvent(b);b=Math.pow(1.2,i);ja(da(c),w([1,0,0,b,0,a-b*a],da(c)));Y();return}}if((a===4||a===5||a===6)&&l.pan){c=j()[4];i=[];for(d=0;d<K();++d)i.push(g(d)[5]);if(a===6)Z({x:-m.pixelX,y:-m.pixelY},0,f,e);else if(a===5)Z({x:0,y:-m.pixelX- m.pixelY},0,f,e);else a===4&&Z({x:-m.pixelX-m.pixelY,y:0},0,f,e);c!==j()[4]&&E.cancelEvent(b);for(d=0;d<K();++d)i[d]!==g(d)[5]&&E.cancelEvent(b)}else if(l.zoom){E.cancelEvent(b);i=-m.spinY;if(i===0)i=-m.spinX;if(a===1)La(d,0,i,f,e);else if(a===0)La(d,i,0,f,e);else if(a===2)La(d,i,i,f,e);else if(a===3)m.pixelX!==0?La(d,i,0,f,e):La(d,0,i,f,e)}}}};var Lb=function(){lb()&&wa.emit(z.widget,\"seriesSelected\",D.x,D.y)};aa.start=function(a,b,c){ha=C(b.touches)===1;ra=C(b.touches)===2;if(ha){Ba=false;var d= E.widgetCoordinates(z.canvas,b.touches[0]);a=Oa(d.x,d.y);var e=Ma(d,h()),f=Na(d.x,d.y);if(a===-1&&!f&&!e)return;Da=a===-1&&!f&&xa()&&Ab(B,[d.x,d.y],30)?1:0;na=Date.now();D=d;t=a;G=f;if(Da!==1){if(!c&&e)ba=window.setTimeout(Lb,200);fa(\"contextmenu\",ta.contextmenuListener)}E.capture(null);E.capture(vb())}else if(ra&&(l.zoom||Ga())){if(ba){window.clearTimeout(ba);ba=null}Ba=false;r=[E.widgetCoordinates(z.canvas,b.touches[0]),E.widgetCoordinates(z.canvas,b.touches[1])].map(function(i){return[i.x,i.y]}); f=false;a=-1;if(!r.every(function(i){return Ma(i,h())})){(f=Na(r[0][v],r[0][u])&&Na(r[1][v],r[1][u]))||(a=Oa(r[0][v],r[0][u]));if(!f&&(a===-1||Oa(r[1][v],r[1][u])!==a)){ra=null;return}G=f;t=a}E.capture(null);E.capture(vb());T=Math.atan2(r[1][1]-r[0][1],r[1][0]-r[0][0]);hb=[(r[0][0]+r[1][0])/2,(r[0][1]+r[1][1])/2];c=Math.abs(Math.sin(T));d=Math.abs(Math.cos(T));T=c<Math.sin(0.125*Math.PI)?0:d<Math.cos(0.375*Math.PI)?Math.PI/2:Math.tan(T)>0?Math.PI/4:-Math.PI/4;ib=Jb(T,hb);G=f;t=a}else return;ya(b)}; aa.end=function(a,b){if(ba){window.clearTimeout(ba);ba=null}window.setTimeout(function(){W(\"contextmenu\",ta.contextmenuListener)},0);var c=Array.prototype.slice.call(b.touches),d=C(c)===0;d||function(){var e;for(e=0;e<C(b.changedTouches);++e)(function(){for(var f=b.changedTouches[e].identifier,i=0;i<C(c);++i)if(c[i].identifier===f){c.splice(i,1);return}})()}();d=C(c)===0;ha=C(c)===1;ra=C(c)===2;if(d){$a=null;if(Da===0&&(isFinite(k.x)||isFinite(k.y))&&l.rubberBand){na=Date.now();Ba=true;Xa(eb)}else{Da=== 1&&qa.mouseUp(null,null);c=[];na=ib=hb=T=null}Da=null}else if(ha||ra)aa.start(a,b,true)};var $a=null,va=null,jb=null;aa.moved=function(a,b){if(ha||ra)if(!(ha&&D==null)){ya(b);va=E.widgetCoordinates(z.canvas,b.touches[0]);if(C(b.touches)>1)jb=E.widgetCoordinates(z.canvas,b.touches[1]);if(!G&&t===-1&&ha&&ba&&!Ab([va.x,va.y],[D.x,D.y],3)){window.clearTimeout(ba);ba=null}$a||($a=setTimeout(function(){if(!G&&t===-1&&ha&&Ga()&&ca(ea())){var c=ea();if(ca(c)){var d=va,e;e=A()?(d.x-D.x)/g(P(ea()))[3]:(d.y- D.y)/g(P(ea()))[3];da(c)[5]+=e;D=d;Y()}}else if(ha){d=va;e=Date.now();var f={x:d.x-D.x,y:d.y-D.y};c=e-na;na=e;if(Da===1){B[v]+=f.x;B[u]+=f.y;xa()&&ma&&Xa(db)}else if(l.pan){k.x=f.x/c;k.y=f.y/c;Z(f,l.rubberBand?zb:0,t,G)}D=d}else if(!G&&t===-1&&ra&&Ga()&&ca(ea())){f=A()?v:u;e=[va,jb].map(function(S){return A()?[S.x,sa]:[Ea,S.y]});c=Math.abs(r[1][f]-r[0][f]);var i=Math.abs(e[1][f]-e[0][f]),m=c>0?i/c:1;if(i===c)m=1;c=ea();if(ca(c)){var sa=w(Ia(ka(P(c))),[0,(r[0][f]+r[1][f])/2])[1],Fa=w(Ia(ka(P(c))), [0,(e[0][f]+e[1][f])/2])[1];ja(da(c),w([1,0,0,m,0,-m*sa+Fa],da(c)));D=d;Y();r=e}}else if(ra&&l.zoom){d=la(B,Q());var Ea=(r[0][0]+r[1][0])/2;sa=(r[0][1]+r[1][1])/2;e=[va,jb].map(function(S){return T===0?[S.x,sa]:T===Math.PI/2?[Ea,S.y]:w(ib,[S.x,S.y])});f=Math.abs(r[1][0]-r[0][0]);c=Math.abs(e[1][0]-e[0][0]);var O=f>0?c/f:1;if(c===f||T===Math.PI/2)O=1;var ab=(e[0][0]+e[1][0])/2;c=Math.abs(r[1][1]-r[0][1]);i=Math.abs(e[1][1]-e[0][1]);m=c>0?i/c:1;if(i===c||T===0)m=1;Fa=(e[0][1]+e[1][1])/2;A()&&function(){var S= O;O=m;m=S;S=ab;ab=Fa;Fa=S;S=Ea;Ea=sa;sa=S}();if(j()[0]*O>ia())O=ia()/j()[0];if(j()[0]*O<Qa())O=Qa()/j()[0];f=[];for(c=0;c<K();++c)f.push(m);for(c=0;c<K();++c){if(g(c)[3]*f[c]>V(c))f[c]=V(c)/g(c)[3];if(g(c)[3]*f[c]<pb(c))f[c]=pb(c)/g(c)[3]}if(G){if(O!==1&&(O<1||j()[0]!==ia()))pa(j(),w([O,0,0,1,-O*Ea+ab,0],j()))}else if(t===-1){if(O!==1&&(O<1||j()[0]!==ia()))pa(j(),w([O,0,0,1,-O*Ea+ab,0],j()));for(c=0;c<K();++c)if(f[c]!==1&&(f[c]<1||g(c)[3]!==V(c)))pa(g(c),w([1,0,0,f[c],0,-f[c]*sa+Fa],g(c)))}else if(f[t]!== 1&&(f[t]<1||g(t)[3]!==V(t)))pa(g(t),w([1,0,0,f[t],0,-f[t]*sa+Fa],g(t)));ga();d=Ua(d,Q());B[v]=d[v];B[u]=d[u];r=e;Ya();Y();Ja()}$a=null},1))}};this.setXRange=function(a,b,c,d){b=o(0)[0]+o(0)[2]*b;c=o(0)[0]+o(0)[2]*c;if(q(o(0))>s(o(0))){if(b>q(o(0)))b=q(o(0));if(c<s(o(0)))c=s(o(0))}else{if(b<q(o(0)))b=q(o(0));if(c>s(o(0)))c=s(o(0))}var e=Pa(a);e=Kb(e,P(a),b,c,A(),h(),o(P(a)),l.minZoom,l.maxZoom);b=e.xZoom;c=e.yZoom;e=e.panPoint;var f=la(B,Q());j()[0]=b;if(c&&d)g(P(a))[3]=c;j()[4]=-e[v]*b;if(c&&d)g(P(a))[5]= -e[u]*c;Ha();a=Ua(f,Q());B[v]=a[v];B[u]=a[u];ga();Ya();Y();Ja()};this.getSeries=function(a){return Pa(a)};this.rangeChangedCallbacks=[];this.updateConfig=function(a){for(var b in a)if(a.hasOwnProperty(b))l[b]=a[b];Fb();Ya();Y();Ja()};this.updateConfig({});if(window.TouchEvent&&!window.MSPointerEvent&&!window.PointerEvent){qa.touchStart=aa.start;qa.touchEnd=aa.end;qa.touchMoved=aa.moved}else{H=function(){};qa.touchStart=H;qa.touchEnd=H;qa.touchMoved=H}}");
	}

	static String locToJsString(AxisValue loc) {
		switch (loc) {
		case MinimumValue:
			return "min";
		case MaximumValue:
			return "max";
		case ZeroValue:
			return "zero";
		case BothSides:
			return "both";
		}
		assert false;
		return "";
	}

	static int binarySearchRow(final WAbstractChartModel model, int xColumn,
			double d, int minRow, int maxRow) {
		if (minRow == maxRow) {
			return minRow;
		}
		double min = model.getData(minRow, xColumn);
		double max = model.getData(maxRow, xColumn);
		if (d <= min) {
			return minRow;
		}
		if (d >= max) {
			return maxRow;
		}
		double start = minRow + (d - min) / (max - min) * (maxRow - minRow);
		double data = model.getData((int) start, xColumn);
		if (data < d) {
			return binarySearchRow(model, xColumn, d, (int) start + 1, maxRow);
		} else {
			if (data > d) {
				return binarySearchRow(model, xColumn, d, minRow,
						(int) start - 1);
			} else {
				return (int) start;
			}
		}
	}

	private static final int TICK_LENGTH = 5;
	private static final int CURVE_LABEL_PADDING = 10;
	private static final int DEFAULT_CURVE_LABEL_WIDTH = 100;
	private static final int CURVE_SELECTION_DISTANCE_SQUARED = 400;

	static int toZoomLevel(double zoomFactor) {
		return (int) Math.floor(Math.log(zoomFactor) / Math.log(2.0) + 0.5) + 1;
	}
}
