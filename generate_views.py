"""
Script to generate CRUD view files for all admin entities
Based on thiet-bi template
"""

entities = {
    'users': {
        'singular': 'người dùng',
        'plural': 'người dùng',
        'api': 'users',
        'fields': {
            'list': ['id', 'hoTen', 'email', 'soDienThoai', 'vaiTro'],
            'form': ['hoTen', 'email', 'matKhau', 'soDienThoai', 'vaiTro']
        }
    },
    'phong-hoc': {
        'singular': 'phòng học',
        'plural': 'phòng học',
        'api': 'phong-hoc',
        'fields': {
            'list': ['id', 'tenPhong', 'toaNha', 'tang', 'sucChua'],
            'form': ['tenPhong', 'toaNha', 'tang', 'sucChua', 'moTa']
        }
    },
    'loai-thiet-bi': {
        'singular': 'loại thiết bị',
        'plural': 'loại thiết bị',
        'api': 'loai-thiet-bi',
        'fields': {
            'list': ['id', 'tenLoai', 'moTa'],
            'form': ['tenLoai', 'moTa']
        }
    },
    'muon-tra': {
        'singular': 'phiếu mượn',
        'plural': 'mượn trả',
        'api': 'muon-tra',
        'fields': {
            'list': ['id', 'nguoiDung', 'thietBi', 'ngayMuon', 'ngayTra', 'trangThai'],
            'form': ['nguoiDungId', 'thietBiId', 'ngayMuon', 'ngayTraDuKien', 'ghiChu']
        }
    },
    'bao-hong': {
        'singular': 'báo hỏng',
        'plural': 'báo hỏng',
        'api': 'bao-hong',
        'fields': {
            'list': ['id', 'thietBi', 'nguoiBao', 'ngayBao', 'mucDo', 'trangThai'],
            'form': ['thietBiId', 'moTa', 'mucDo']
        }
    },
    'bao-tri': {
        'singular': 'phiếu bảo trì',
        'plural': 'bảo trì',
        'api': 'bao-tri',
        'fields': {
            'list': ['id', 'thietBi', 'ngayBaoTri', 'loaiBaoTri', 'nguoiThucHien', 'ketQua'],
            'form': ['thietBiId', 'ngayBaoTri', 'loaiBaoTri', 'noiDung', 'nguoiThucHien', 'chiPhi']
        }
    }
}

print("Entity configuration loaded")
print(f"Total entities: {len(entities)}")
print(f"Total files to generate: {len(entities) * 4}")
