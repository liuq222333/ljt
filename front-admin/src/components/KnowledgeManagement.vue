<template>
  <div class="knowledge-management">
    <h2>知识库管理</h2>

    <div class="toolbar">
      <el-button type="primary" @click="showAddDialog">添加知识</el-button>
      <el-select v-model="filterCategory" placeholder="分类筛选" clearable @change="loadList">
        <el-option label="全部" value=""></el-option>
        <el-option label="常见问题" value="faq"></el-option>
        <el-option label="使用指南" value="guide"></el-option>
        <el-option label="业务规则" value="rule"></el-option>
        <el-option label="政策说明" value="policy"></el-option>
      </el-select>
    </div>

    <el-table :data="knowledgeList" border style="margin-top: 20px">
      <el-table-column prop="id" label="ID" width="80"></el-table-column>
      <el-table-column prop="category" label="分类" width="100">
        <template #default="scope">
          <el-tag>{{ getCategoryName(scope.row.category) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" width="200"></el-table-column>
      <el-table-column prop="content" label="内容" show-overflow-tooltip></el-table-column>
      <el-table-column prop="viewCount" label="查看次数" width="100"></el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button size="small" @click="editKnowledge(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteKnowledge(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      @current-change="handlePageChange"
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 20px; text-align: right">
    </el-pagination>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="请选择分类">
            <el-option label="常见问题" value="faq"></el-option>
            <el-option label="使用指南" value="guide"></el-option>
            <el-option label="业务规则" value="rule"></el-option>
            <el-option label="政策说明" value="policy"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入标题"></el-input>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入内容"></el-input>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keywords" placeholder="多个关键词用逗号分隔"></el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0"></el-switch>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveKnowledge">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'KnowledgeManagement',
  data() {
    return {
      knowledgeList: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      filterCategory: '',
      dialogVisible: false,
      dialogTitle: '添加知识',
      form: {
        id: null,
        category: '',
        title: '',
        content: '',
        keywords: '',
        status: 1
      }
    };
  },
  mounted() {
    this.loadList();
  },
  methods: {
    async loadList() {
      try {
        const res = await axios.get('/api/knowledge/list', {
          params: {
            page: this.currentPage,
            size: this.pageSize,
            category: this.filterCategory
          }
        });
        if (res.data.success) {
          this.knowledgeList = res.data.data.records;
          this.total = res.data.data.total;
        }
      } catch (error) {
        this.$message.error('加载失败');
      }
    },
    showAddDialog() {
      this.dialogTitle = '添加知识';
      this.form = { id: null, category: '', title: '', content: '', keywords: '', status: 1 };
      this.dialogVisible = true;
    },
    editKnowledge(row) {
      this.dialogTitle = '编辑知识';
      this.form = { ...row };
      this.dialogVisible = true;
    },
    async saveKnowledge() {
      try {
        if (this.form.id) {
          await axios.put(`/api/knowledge/${this.form.id}`, this.form);
        } else {
          await axios.post('/api/knowledge/add', this.form);
        }
        this.$message.success('保存成功');
        this.dialogVisible = false;
        this.loadList();
      } catch (error) {
        this.$message.error('保存失败');
      }
    },
    async deleteKnowledge(id) {
      try {
        await this.$confirm('确认删除？', '提示', { type: 'warning' });
        await axios.delete(`/api/knowledge/${id}`);
        this.$message.success('删除成功');
        this.loadList();
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('删除失败');
        }
      }
    },
    handlePageChange(page) {
      this.currentPage = page;
      this.loadList();
    },
    getCategoryName(category) {
      const map = { faq: '常见问题', guide: '使用指南', rule: '业务规则', policy: '政策说明' };
      return map[category] || category;
    }
  }
};
</script>

<style scoped>
.knowledge-management {
  padding: 20px;
}
.toolbar {
  display: flex;
  gap: 10px;
}
</style>
