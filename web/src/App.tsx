import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import HomeScreen from './screens/HomeScreen';
import AddItemScreen from './screens/AddItemScreen';
import ItemDetailScreen from './screens/ItemDetailScreen';
import StatisticsScreen from './screens/StatisticsScreen';
import SettingsScreen from './screens/SettingsScreen';

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<HomeScreen />} />
        <Route path="/add" element={<AddItemScreen />} />
        <Route path="/item/:itemId" element={<ItemDetailScreen />} />
        <Route path="/statistics" element={<StatisticsScreen />} />
        <Route path="/settings" element={<SettingsScreen />} />
      </Routes>
    </Layout>
  );
}
