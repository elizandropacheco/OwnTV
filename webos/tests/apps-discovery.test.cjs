const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
function setup(config){
 const sockets=[],saved=[],timers=new Map();let seq=0;
 const context={setTimeout(fn){timers.set(++seq,fn);return seq;},clearTimeout(id){timers.delete(id);},PalmServiceBridge:function(){this.call=(uri,raw)=>{const p=JSON.parse(raw);if(uri.endsWith('/setAppsKey'))saved.push(p.key);this.onservicecallback(JSON.stringify(uri.endsWith('/appsConnection')?config:{returnValue:true}));};},WebSocket:function(url){this.url=url;this.sent=[];this.send=x=>this.sent.push(JSON.parse(x));this.close=()=>{this.closed=true;};sockets.push(this);}};
 vm.runInNewContext(fs.readFileSync(require.resolve('../app/apps-discovery'),'utf8'),context);
 let result,status=[];context.OwnTVApps.discover(false,x=>status.push(x),(error,apps)=>result={error,apps});
 return {sockets,saved,status,get result(){return result;},context};
}
let t=setup({returnValue:true,hosts:['192.168.1.20'],key:''});
assert.ok(t.result.error);assert.equal(t.sockets.length,0);
t=setup({returnValue:true,hosts:['http://untrusted.invalid','192.168.1.20'],key:'test-valid-key'});
let s=t.sockets[0];assert.equal(s.url,'ws://192.168.1.20:3000');s.onopen();assert.equal(s.sent[0].payload['client-key'],'test-valid-key');
s.onmessage({data:JSON.stringify({type:'registered',payload:{'client-key':'test-valid-key'}})});
assert.deepEqual(t.saved,['test-valid-key']);assert.equal(s.sent[1].uri,'ssap://com.webos.applicationManager/listLaunchPoints');
s.onmessage({data:JSON.stringify({id:'apps',payload:{launchPoints:[{id:'netflix',title:'Netflix'}]}})});
assert.equal(t.result.error,null);assert.equal(t.result.apps[0].id,'netflix');assert.equal(s.closed,true);
t=setup({returnValue:true,hosts:['192.168.1.20'],key:'test-valid-key'});s=t.sockets[0];s.onmessage({data:'{bad'});assert.ok(t.result.error);assert.equal(s.closed,true);
t=setup({returnValue:true,hosts:['192.168.1.20'],key:'test-valid-key'});s=t.sockets[0];s.onmessage({data:JSON.stringify({payload:{pairingType:'PROMPT'}})});assert.ok(t.result.error);assert.equal(s.closed,true);assert.equal(t.saved.length,0);
t=setup({returnValue:true,hosts:['192.168.1.20'],key:'test-valid-key'});s=t.sockets[0];s.onmessage({data:JSON.stringify({type:'registered',payload:{}})});assert.ok(t.result.error);assert.equal(t.saved.length,0);
console.log('Discovery: TV-local endpoints, no unsolicited pairing, private key persistence, invalid responses and revoked-key handling passed.');
