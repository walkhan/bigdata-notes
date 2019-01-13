## 设置用户名和邮箱
```
git config --global user.name 'your_name'
git config --global user.email 'your_email'
ssh-keygen -t rsa -C 'your_email'
```
生成SSH密匙 ssh-keygen -t rsa -C 'your_email',email和你设置的git邮箱一致
## 测试是否关联成功
```
ssh git@github.com
```
## 同步github到本地仓库
```
git clone 项目地址
git clone https://github.com/walkhan/bigdata-notes.git                        
```

拉取到本地很简单，主要是上传到github涉及的命令较多
## 同步本地仓库到github
### 初始化git仓库
```
git init
```
### 把所有项目文件添加到提交暂存区
```
git add .
```
### 把暂存区中的内容提交到仓库
```
git commit -m '提交说明'
```
 接下来，github创建相应的项目仓库
### 同步本地仓库到远程仓库
```
git remote add origin git@github.com:[githubUerName]/[resName]
```
### 本地仓库内容push到远程仓库的master分支
```
git push -u origin master
```
-f:强制覆盖
错误：error: failed to push some refs to  git@github.com:walkhan/idmskafka.git
### 通过如下命令进行代码合并
```
git pull --rebase origin master
```
再次同步成功
### **移除文件夹**
```
git rm -r --cached  "文件夹"
git commit -m 'delete class file'
git push origin master
```
再次提交成功
## **改动后的项目要上传到github**
### 将项目的所有文件添加到仓库中
```
git add .
```
如果要上传单独一个文件，‘.’改为需要上传的文件名即可
### 将add的文件commit到仓库
```
git commit -m '注释'
```
### git push 上传，可能会让你输入github账号和密码按提示输入即可
```
git push
```
### git移除add的文件
```
 git rm –cached “文件路径”
```
不删除物理文件，仅将该文件从缓存中删除
```
git rm –f “文件路径”
```
不仅将该文件从缓存中删除，还会将物理文件删
### 取消缓存区的修改
```
git reset HEAD
```
不添加参数，撤销所有缓存区的修改
### git commit已提交的Author信息可以通过git log查看
```
git log
```
## 关闭git忽略大小写配置
```
git config core.ignorecase false
```

