import api from './axios';

const tipPoolApi = {
    createPool: async (poolData) => {
        const response = await api.post('/api/tip-pools', poolData);
        return response.data;
    },

    getPools: async () => {
        const response = await api.get('/api/tip-pools');
        return response.data;
    },

    getPoolById: async (id) => {
        const response = await api.get(`/api/tip-pools/${id}`);
        return response.data;
    },

    deletePool: async (id) => {
        const response = await api.delete(`/api/tip-pools/${id}`);
        return response.data;
    },

    finalizePool: async (id) => {
        const response = await api.post(`/api/tip-pools/${id}/finalize`);
        return response.data;
    }
};

export default tipPoolApi;
