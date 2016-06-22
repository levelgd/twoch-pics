var request = require('request');
var express = require('express');
var queue = require('queue');
var app = express();

//request = request.defaults({ jar: true });

/*
    полная база борд - ассоциативный массив, dvachbase[борданейм] =
    [ массив тредов, 0 - "первый тред", обычно закрепленный, объект
        { тред
            title:заголовок,
            id:номер первого поста по которому можно сформировать ссылку на тред,
            post:первый пост как он есть,
            upd:время последнего обновления,
            pics:[ картиночки, массив элементов
                {
                    thumb:ссылка на превью,
                    name:название картинки,
                    img:ссылка на картинку,
                    w:ширина картинки в пикселях,
                    h:высота картинки в пикселях,
                    size:размер файла в килобайтах,
                    ext:расширение
                }
            ]
        }
    ]
*/
var actualVersion = "0.3";
var clientVersion = "";

var dvachbase = {};

app.set('view engine', 'ejs');
app.use(express.static('public'));

app.get('/', function(req, res) { // запрос: хост?board=борданеймбезслешей&thread=порядковыйномертреда,новыевверху

    if(!req.query.version){
        clientVersion = "deprecated";
    }else{
        clientVersion = req.query.version;
    }

    if(dvachbase.hasOwnProperty(req.query.board)){

        var thr = findThreadById(req.query.board,req.query.thread);

        if(thr){
            res.render('indexnew',{ //для ejs рендерера передаем объект всего треда
                thread:thr,
                borda:req.query.board,
                av:actualVersion,
                cv:clientVersion,
            });

            /*var ip = req.headers['x-forwarded-for'] || req.connection.remoteAddress;
            request("http://levelgd.ru/pikchi/stat.php?ip="+ip+"&ver="+clientVersion); // использовалось для статы. */ 

        }else{
            res.status(404).send("<h1 style='text-align: center'>тред не найден</h1>");
        }
    }else{
        res.render('about',{
            av:actualVersion
        });
    }
});

app.get('/count',function(req, res){
   if(dvachbase.hasOwnProperty(req.query.board)){

       var l = dvachbase[req.query.board].length - 1;
       var str = "";

       dvachbase[req.query.board].forEach(function(t, i){
           str += t.id;
           if(i < l) str += ",";
       });

       res.status(200).send(str);
   }else{
       res.status(200).send("0");
   }
});

app.get('/post', function(req, res){

    if(dvachbase.hasOwnProperty(req.query.board)){

        var thr = findThreadById(req.query.board,req.query.thread);
        var thum = req.query.thumblink;

        console.log(thum);

        var post = "0";
        thr.pics.forEach(function(p){
            if(p.thumb == thum || p.img == thum){
                post = "https://2ch.hk/" + req.query.board + "/res/" + req.query.thread + ".html#" + p.post;
            }
        });

        res.status(200).send(post);

    }else{
        res.status(200).send("0");
    }

});

app.get('/threads', function(req, res){
    if(dvachbase.hasOwnProperty(req.query.board)){

        var l = dvachbase[req.query.board].length - 1;
        var str = "";

        dvachbase[req.query.board].forEach(function(t, i){
           str += ("№" + t.id + "¶" + t.title + "¶" + t.post);
           if(i < l) str += "•";
       });

       res.status(200).send(str);

    }else{
        res.status(200).send("");
    }
});

app.use(function(req, res, next){
    res.status(404).send("<h1 style='text-align: center'>404 azaza</h1>");
});

app.listen((process.env.PORT || 5000), function () {
    console.log("~~~ ZAGRUZKA MEMESOV POSHLA ~~~");//
});

var boards = [//b - название борды, m - частота обновления (каждые m минут), t - число выводимых тредов
    {b:"b", m:10, t:20, updating:false},
    {b:"po", m:10, t:10, updating:false},
    {b:"vg", m:10, t:10, updating:false},
    {b:"soc", m:20, t:10, updating:false},
    {b:"media", m:20, t:10, updating:false},
    {b:"spc", m:20, t:10, updating:false},
    {b:"wm", m:20, t:10, updating:false},
    {b:"pa", m:20, t:10, updating:false},
    {b:"p", m:20, t:10, updating:false},
    {b:"c", m:20, t:10, updating:false},
    {b:"mlp", m:20, t:10, updating:false},
    {b:"a", m:20, t:10, updating:false},
    {b:"ma", m:20, t:10, updating:false},
    {b:"h", m:20, t:10, updating:false},
    {b:"ho", m:20, t:10, updating:false},
    {b:"gg", m:20, t:10, updating:false},
    {b:"e", m:20, t:10, updating:false},
    {b:"fag", m:20, t:10, updating:false},
    {b:"rf", m:20, t:10, updating:false}
];

boards.forEach(function(board, i){

    setTimeout(function(){

        loadBoard(board.b, board.t, board);

        setInterval(function(){
            if(!board.updating){
                loadBoard(board.b, board.t, board);
            }
        }, board.m * 60000);

    },i * 3000);

});

function loadBoard(name, maxthreads, brd){

    brd.updating = true;

    console.log("start loading board: /" + name + "/");

    request({
        url:"https://2ch.hk/"+name+"/catalog.json",
        headers:{
            "User-Agent":"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36",
            "Cookie":"usercode_auth=270f74911b5eab78ff03889ae3bda118"
        } // usercode_auth нужен для доступа к разделам 18+. Юзер-агент - маскировка под браузер.
    }, function (error, response, body){
        if (!error && response.statusCode == 200) {
            try{
                var ths = JSON.parse(body).threads;
            }catch(e){
                console.log("error loading board catalog " +e);
                brd.updating = false;////////////////
                return;
            }

            var threads = [];

            ths.forEach(function(th){

                if(threads.length < maxthreads){
                    threads.push({
                        title:th.subject,
                        post:th.comment.replace(/<br>.*/,"..."),
                        id:th.num,
                        upd:" - в процессе",
                        pics:[]
                    });
                }
            });

            var q = queue();

            threads.forEach(function(t, i){

                q.push(function(cb){

                    request({
                        url:'https://2ch.hk/'+name+'/res/'+t.id+'.json',
                        headers:{
                            "User-Agent":"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36",
                            "Cookie":"usercode_auth=270f74911b5eab78ff03889ae3bda118"
                        }
                    }, function (error, response, body) {
                      if (!error && response.statusCode == 200) {

                          var skip = false;
                          var posts = undefined;

                          try{
                              posts = JSON.parse(body).threads[0];
                          }catch(e){
                              console.log("error loading thread: " + e);
                              posts = undefined;
                          }

                          if(!skip && posts && posts.posts){
                              posts.posts.forEach(function(p){

                                 //console.log(p);

                                 if(p.files.length > 0){
                                     p.files.forEach(function(f){

                                         var data = {
                                             post:p.num,
                                             thumb:"https://2ch.hk/"+name+"/"+f.thumbnail,
                                             name:f.name,
                                             img:"https://2ch.hk/"+name+"/"+f.path,
                                             w:f.width,
                                             h:f.height,
                                             size:f.size,
                                             ext:f.name.replace(/^.*\./,"")
                                         };

                                         t.pics.push(data);
                                     });
                                 }
                              });

                              t.pics.reverse();
                              t.upd = addTime();

                              // Это уже граббер картиночек.
                              /*download.forEach(function(d){
                                 //console.log(d);
                                 q.push(function(cb) {

                                     request({
                                                url:d.path,
                                                headers:{ "User-Agent":"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36" },
                                                encoding: "binary"
                                             }, function(error, response, data){
                                         if(!error && response && response.statusCode == 200){

                                             fs.writeFileSync("dl/"+d.name, data, 'binary');

                                             console.log(d.name + " success");
                                         }else{
                                             console.log(d.name + " error " + error + " with status " + response.statusCode);
                                         }

                                         cb();

                                     });//.pipe(fs.createWriteStream("dl/"+d.name));
                                 });
                              });

                              q.timeout = 10000;

                              q.start(function(err) {
                                 console.log("ok");
                              });*/
                          }
                      }else{
                          if(error){
                              console.log("2ch.hk thread error " + error + " (/" + name + "/)");
                          }else{
                              console.log("2ch.hk thread response " + response.statusCode + " (/" + name + "/)");
                          }
                      }

                      cb();

                    });
                });
            });

            q.concurrency = 1;
            q.timeout = 30000;

            q.on("timeout",function(next,job){
                console.log("timeout... ");
                next();
            });

            q.start(function(err){
                if(!err){
                    dvachbase[name] = threads;
                    console.log("finished: /" + name + "/");
                }else{
                    console.log(err);
                }

                brd.updating = false;////////////////
            });
        }else{
            if(error){
                console.log("/"+name+"/ 2ch.hk catalog.json error " + error);
            }else{
                console.log("/"+name+"/ 2ch.hk catalog.json responce " + response.statusCode);
            }

            brd.updating = false;////////////////
        }
    });
}

function findThreadById(board,id){

    var thread = undefined;

    if(dvachbase.hasOwnProperty(board)){
        dvachbase[board].forEach(function(t){
            if(t.id == id) thread = t;
        });
    }

    return thread;
}

function addTime(){
    var d = new Date();
    return d.toDateString().slice(4) + ", " + d.toTimeString().slice(0,8);
}
